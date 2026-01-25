package com.leagueplans.scraper.dumper.items

import com.leagueplans.common.model.Item
import com.leagueplans.scraper.dumper.{ImageDumper, JsonDumper}
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.telemetry.Metric
import com.leagueplans.scraper.wiki.model.WikiItem.GameID
import com.leagueplans.scraper.wiki.model.{InfoboxVersion, Page, PageDescriptor, WikiItem}
import io.circe.parser.decode
import zio.stream.{ZPipeline, ZSink}
import zio.{Chunk, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import scala.util.Try

object ItemDumper {
  private type PipelineOutput = (wikiKey: WikiKey, item: Item, images: Chunk[(Path, Array[Byte])])
  
  def make(args: CommandLineArgs, targetDirectory: Path)(using Trace): Task[ItemDumper] = {
    for {
      dumpDirectory <- ZIO.fromTry(makeDumpDirectory(targetDirectory))
      (idAllocator, idMapDumper) <- makeIDMapParams(args)
      dataDumper <- ZIO.fromTry(makeDataDumper(dumpDirectory))
      iconDumper <- makeIconDumper(dumpDirectory)
      itemCounter <- Metric.makeCounter("items.item-dumper.items")
    } yield ItemDumper(idAllocator, dataDumper, idMapDumper, iconDumper, itemCounter)
  }
  
  private def makeDumpDirectory(targetDirectory: Path): Try[Path] =
    for {
      dumpDirectory <- Try(targetDirectory.resolve("dump"))
      _ <- Try(Files.createDirectories(dumpDirectory))
    } yield dumpDirectory

  private def makeIDMapParams(
    args: CommandLineArgs
  )(using Trace): Task[(ItemIDAllocator, JsonDumper[Vector[(WikiKey, Item.ID)]])] =
    for {
      idMapFile <- ZIO.fromTry(args.get("id-map")(path => Try(Path.of(path))))
      idMap <- ZIO.fromTry(createOrGet(idMapFile))
      idAllocator <- ItemIDAllocator.make(idMap)
      idMapDumper <- ZIO.fromTry(JsonDumper.make[Vector[(WikiKey, Item.ID)]](idMapFile))
    } yield (idAllocator, idMapDumper)
    
  private def makeDataDumper(dumpDirectory: Path): Try[JsonDumper[Vector[Item]]] =
    for {
      itemsFile <- Try(dumpDirectory.resolve("data/items.json"))
      _ <- Try(Files.createDirectories(itemsFile.getParent))
      dataDumper <- JsonDumper.make[Vector[Item]](itemsFile)
    } yield dataDumper
  
  private def makeIconDumper(dumpDirectory: Path)(using Trace): Task[ImageDumper] =
    for {
      imagesDirectory <- ZIO.attempt(dumpDirectory.resolve("dynamic/assets/images/items"))
      imageDumper <- ImageDumper.make("items", imagesDirectory)
    } yield imageDumper
    
  private def createOrGet(idMapFile: Path): Try[Map[WikiKey, Item.ID]] =
    Try(Files.exists(idMapFile)).flatMap {
      case true =>
        for {
          contents <- Try(Files.readString(idMapFile))
          idMap <- decode[Vector[(WikiKey, Item.ID)]](contents).toTry
          _ <- Try(Files.delete(idMapFile))
        } yield idMap.toMap

      case false =>
        Try(Files.createDirectories(idMapFile.getParent)).map(Map.empty)
    }
}

final class ItemDumper(
  idAllocator: ItemIDAllocator,
  dataDumper: JsonDumper[Vector[Item]],
  idMapDumper: JsonDumper[Vector[(WikiKey, Item.ID)]],
  iconDumper: ImageDumper,
  itemCounter: Metric.Counter[Long]
) {
  def sink(using Trace): ZSink[Any, Throwable, Page[WikiItem], Nothing, Unit] =
    pipeline.tap(_ => itemCounter.increment) >>> (dataSink <&> idMapSink <&> iconSink)

  private def pipeline(using Trace): ZPipeline[Any, Nothing, Page[WikiItem], ItemDumper.PipelineOutput] =
    ZPipeline.mapZIO((page, wikiItem) =>
      val wikiKey = (page.id, wikiItem.infoboxes.version)
      idAllocator.get(wikiKey).map(id =>
        (wikiKey, toItem(id, wikiItem), toImages(id, wikiItem))
      )
    )

  private def dataSink(using Trace): ZSink[Any, Throwable, ItemDumper.PipelineOutput, Nothing, Unit] =
    ZSink
      .collectAll[Item]
      .contramap[ItemDumper.PipelineOutput](_.item)
      .mapZIO(items => ZIO.fromTry(dataDumper.dump(items.toVector.sorted)))

  private def idMapSink(using Trace): ZSink[Any, Throwable, ItemDumper.PipelineOutput, Nothing, Unit] =
    ZSink
      .collectAll[(WikiKey, Item.ID)]
      .contramap[ItemDumper.PipelineOutput](data => data.wikiKey -> data.item.id)
      .mapZIO(data => ZIO.fromTry(idMapDumper.dump(data.toVector)))

  private def iconSink(using Trace): ZSink[Any, Throwable, ItemDumper.PipelineOutput, Nothing, Unit] =
    ZSink
      .foreach[Any, Throwable, (Path, Array[Byte])]((path, icon) => iconDumper.dump(path, icon))
      .contramapChunks[Chunk[(Path, Array[Byte])]](_.flatten)
      .contramap[ItemDumper.PipelineOutput](data => Chunk.fromIterator(data.images.iterator))

  private def toItem(id: Item.ID, wikiItem: WikiItem): Item =
    Item(
      id,
      toLiveID(wikiItem.infoboxes.item.id),
      toName(wikiItem.infoboxes),
      wikiItem.infoboxes.item.examine,
      wikiItem.images.map(image => (image.bin, Item.Image.Path(toImagePath(id, image)))),
      wikiItem.infoboxes.item.bankable,
      wikiItem.infoboxes.item.stackable,
      wikiItem.infoboxes.item.noteable,
      wikiItem.infoboxes.maybeBonuses.map(_.equipmentType)
    )

  private def toName(infoboxes: WikiItem.Infoboxes): String =
    infoboxes.version.raw match {
      case Nil => infoboxes.pageName.wikiName
      case path => s"${infoboxes.pageName.wikiName} (${path.mkString(", ")})"
    }

  private def toLiveID(id: WikiItem.GameID): Option[Int] =
    id match {
      case GameID.Live(raw) => Some(raw)
      case _ => None
    }

  private def toImages(id: Item.ID, item: WikiItem): Chunk[(Path, Array[Byte])] =
    Chunk.fromIterator(
      item.images.iterator.map(image => Path.of(toImagePath(id, image)) -> image.data)
    )

  private def toImagePath(id: Item.ID, image: WikiItem.Image): String =
    s"$id/${image.bin.floor}.${image.fileName.extension}"
}
