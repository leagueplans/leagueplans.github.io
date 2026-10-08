package com.leagueplans.scraper.dumper.items

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}
import com.leagueplans.scraper.dumper.{ImageDumper, JsonDumper}
import com.leagueplans.scraper.telemetry.Metric
import com.leagueplans.scraper.wiki.model.WikiItem.GameID
import com.leagueplans.scraper.wiki.model.{Page, PageDescriptor, WikiItem}
import com.leagueplans.scraper.wiki.streaming.PageStream
import zio.http.Request
import zio.stream.{ZPipeline, ZSink}
import zio.{Chunk, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import scala.util.Try

object ItemDumper {
  private type PipelineOutput = (key: InfoboxKey, item: ItemData, images: Chunk[(Path, Array[Byte])])

  private type DescribedImage = (image: ItemData.Image, maybeData: Option[Array[Byte]])

  def make(
    originalItems: Map[InfoboxKey, ItemData],
    targetDirectory: Path
  )(using Trace): Task[ItemDumper] =
    for {
      dumpDirectory <- ZIO.fromTry(makeDirectories(targetDirectory, "dump"))
      changesetDumper <- ZIO.fromTry(makeChangesetDumper(dumpDirectory))
      iconDumper <- makeIconDumper(dumpDirectory)
      itemCounter <- Metric.makeCounter("items.item-dumper.items")
    } yield ItemDumper(originalItems, changesetDumper, iconDumper, itemCounter)

  private def makeDirectories(targetDirectory: Path, relativePath: String): Try[Path] =
    for {
      directory <- Try(targetDirectory.resolve(relativePath))
      _ <- Try(Files.createDirectories(directory))
    } yield directory

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
      // the decision to write it out cannot fall out of step. An icon that wasn't downloaded
      // is the accepted one, and has no bytes to write.
      val images: NonEmptyList[ItemDumper.DescribedImage] =
        wikiItem.images.map(image =>
          image.content match {
            case WikiItem.Image.Content.Downloaded(data) =>
              (ItemData.Image(image.bin, image.fileName.extension, ImageHash.of(data), image.wikiSHA1), Some(data))
            case WikiItem.Image.Content.Accepted(hash) =>
              (ItemData.Image(image.bin, image.fileName.extension, hash, image.wikiSHA1), None)
          }
        )

      (infoboxKey, toItemData(wikiItem, images.map(_.image)), toImages(infoboxKey, images))
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
      // The infobox version is kept apart, in the item's InfoboxKey
      item.infoboxes.pageName.wikiName,
      item.infoboxes.item.examine,
      images,
      item.infoboxes.item.bankable,
      item.infoboxes.item.stackable,
      item.infoboxes.item.noteable,
      item.infoboxes.maybeBonuses.map(_.equipmentType)
    )

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
    images: NonEmptyList[ItemDumper.DescribedImage]
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
          case (image, Some(data)) if !acceptedHashes.get(image.bin.floor).contains(image.hash) =>
            Path.of(s"${ItemChangeset.imageDirectory(key)}/${image.fileName}") -> data
        }
    )
  }
}
