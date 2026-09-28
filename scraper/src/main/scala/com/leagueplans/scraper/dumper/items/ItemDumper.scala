package com.leagueplans.scraper.dumper.items

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}
import com.leagueplans.scraper.dumper.{ImageDumper, JsonDumper}
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.telemetry.Metric
import com.leagueplans.scraper.wiki.model.WikiItem.GameID
import com.leagueplans.scraper.wiki.model.{Page, PageDescriptor, WikiItem}
import com.leagueplans.scraper.wiki.streaming.PageStream
import io.circe.parser.decode
import zio.http.Request
import zio.stream.{ZPipeline, ZSink}
import zio.{Chunk, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import scala.util.{Success, Try}

object ItemDumper {
  private type PipelineOutput = (key: InfoboxKey, item: ItemData, images: Chunk[(Path, Array[Byte])])

  def make(args: CommandLineArgs, targetDirectory: Path)(using Trace): Task[ItemDumper] = {
    for {
      dataPath <- ZIO.fromTry(args.get("original-items")(path => Try(Path.of(path))))
      originalItems <- ZIO.fromTry(loadOriginalData(dataPath))
      dumpDirectory <- ZIO.fromTry(makeDirectories(targetDirectory, "dump"))
      changesetDumper <- ZIO.fromTry(makeChangesetDumper(dumpDirectory))
      iconDumper <- makeIconDumper(dumpDirectory)
      itemCounter <- Metric.makeCounter("items.item-dumper.items")
    } yield ItemDumper(originalItems, changesetDumper, iconDumper, itemCounter)
  }

  private def makeDirectories(targetDirectory: Path, relativePath: String): Try[Path] =
    for {
      directory <- Try(targetDirectory.resolve(relativePath))
      _ <- Try(Files.createDirectories(directory))
    } yield directory

  private def loadOriginalData(path: Path): Try[Map[InfoboxKey, ItemData]] =
    Try(Files.exists(path)).flatMap {
      case true =>
        for {
          contents <- Try(Files.readString(path))
          data <- decode[Vector[(InfoboxKey, ItemData)]](contents).toTry
        } yield data.toMap

      case false => Success(Map.empty)
    }

  private def makeChangesetDumper(directory: Path): Try[JsonDumper[ItemChangeset]] =
    for {
      dataDirectory <- makeDirectories(directory, "data")
      path <- Try(dataDirectory.resolve("changeset.json"))
      dumper <- JsonDumper.make[ItemChangeset](path)
    } yield dumper

  private def makeIconDumper(dumpDirectory: Path)(using Trace): Task[ImageDumper] =
    for {
      imagesDirectory <- ZIO.attempt(dumpDirectory.resolve("dynamic/assets/images/items"))
      imageDumper <- ImageDumper.make("items", imagesDirectory)
    } yield imageDumper
}

final class ItemDumper(
  originalItems: Map[InfoboxKey, ItemData],
  changesetDumper: JsonDumper[ItemChangeset],
  iconDumper: ImageDumper,
  itemCounter: Metric.Counter[Long]
) {
  /** Writes out the icons, and collects the items for [[dumpChangeset]]. */
  def sink(using Trace): ZSink[Any, Throwable, Page[WikiItem], Nothing, Map[InfoboxKey, ItemData]] =
    pipeline.tap(_ => itemCounter.increment) >>> (itemSink <&> iconSink)

  /** Written only once the scrape's errors are known as well as its items, since a page
    * that failed to parse must not be reported as removed.
    */
  def dumpChangeset(
    items: Map[InfoboxKey, ItemData],
    errors: Chunk[PageStream.Error]
  ): Task[Unit] = {
    val (failedPages, failedRequests) =
      errors.toList.partitionMap {
        case (page: PageDescriptor, _) => Left(page.id: Int)
        case (request: Request, _) => Right(s"${request.method.render} ${request.url.encode}")
      }

    val changeset =
      ChangesetComputer.compute(originalItems, items, failedPages.toSet, failedRequests.distinct)

    ZIO.fromTry(changesetDumper.dump(changeset))
  }

  private def pipeline(using Trace): ZPipeline[Any, Nothing, Page[WikiItem], ItemDumper.PipelineOutput] =
    ZPipeline.map((page, wikiItem) =>
      val infoboxKey = InfoboxKey(page.id, wikiItem.infoboxes.version.raw)
      // Hashed once and kept alongside its bytes, so that the description of an image and
      // the decision to write it out cannot fall out of step.
      val images =
        wikiItem.images.map(image =>
          (ItemData.Image(image.bin, image.fileName.extension, ImageHash.of(image.data)), image.data)
        )

      (infoboxKey, toItemData(wikiItem, images.map(_._1)), toImages(infoboxKey, images))
    )

  private def itemSink(using Trace): ZSink[Any, Nothing, ItemDumper.PipelineOutput, Nothing, Map[InfoboxKey, ItemData]] =
    ZSink
      .collectAll[(InfoboxKey, ItemData)]
      .contramap[ItemDumper.PipelineOutput](data => data.key -> data.item)
      .map(_.toMap)

  private def iconSink(using Trace): ZSink[Any, Throwable, ItemDumper.PipelineOutput, Nothing, Unit] =
    ZSink
      .foreach[Any, Throwable, (Path, Array[Byte])]((path, icon) => iconDumper.dump(path, icon))
      .contramapChunks[Chunk[(Path, Array[Byte])]](_.flatten)
      .contramap[ItemDumper.PipelineOutput](data => Chunk.fromIterator(data.images.iterator))

  private def toItemData(item: WikiItem, images: NonEmptyList[ItemData.Image]): ItemData =
    ItemData(
      toLiveID(item.infoboxes.item.id),
      toName(item.infoboxes),
      item.infoboxes.item.examine,
      images,
      item.infoboxes.item.bankable,
      item.infoboxes.item.stackable,
      item.infoboxes.item.noteable,
      item.infoboxes.maybeBonuses.map(_.equipmentType)
    )

  private def toName(infoboxes: WikiItem.Infoboxes): String =
    infoboxes.version.raw match {
      case Nil => infoboxes.pageName.wikiName
      case path => s"${infoboxes.pageName.wikiName} (${path.mkString(", ")})"
    }

  private def toLiveID(id: Option[WikiItem.GameID]): Option[Int] =
    id match {
      case Some(GameID.Live(raw)) => Some(raw)
      case _ => None
    }

  /** The images to write out for this item, which is only those that have actually
    * changed.
    *
    * A run dumps every image it downloads unless told otherwise, which produces tens of
    * thousands of files whose contents are already on disk. Comparing against the accepted
    * data leaves only the images a reviewer has yet to see.
    */
  private def toImages(
    key: InfoboxKey,
    images: NonEmptyList[(ItemData.Image, Array[Byte])]
  ): Chunk[(Path, Array[Byte])] = {
    val acceptedHashes =
      originalItems
        .get(key)
        .fold(Map.empty[Int, String])(_.images.toList.map(i => i.bin.floor -> i.hash).toMap)

    Chunk.fromIterator(
      images
        .toList
        .iterator
        .collect {
          case (image, data) if !acceptedHashes.get(image.bin.floor).contains(image.hash) =>
            Path.of(s"${ItemChangeset.imageDirectory(key)}/${image.fileName}") -> data
        }
    )
  }
}
