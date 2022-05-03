package ddm.scraper.dumper

import akka.stream.scaladsl.{Flow, Keep, Sink}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.model.{Page, WikiItem}
import io.circe.Decoder
import io.circe.parser.decode

import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object ItemDumper {
  private type WikiKey = (Page.ID, Option[String])
  private type Output = (WikiKey, Item, NonEmptyList[(Item.Image.Path, Array[Byte])])

  def dump(
    idMapPath: Path,
    itemDataTarget: Path,
    imagesRootTarget: Path
  )(implicit ec: ExecutionContext): Sink[(Page, WikiItem), Future[_]] = {
    val idMap = load[Map[WikiKey, Item.ID]](idMapPath, default = Map.empty)

    Flow[(Page, WikiItem)]
      .map { case (page, wikiItem) =>
        val wikiKey = (page.id, wikiItem.infobox.version)
        val itemID = idMap.getOrElse(wikiKey, Item.ID(UUID.randomUUID()))
        toOutput(itemID, page.id, wikiItem)
      }
      .toMat(outputSink(idMapPath, itemDataTarget, imagesRootTarget))(Keep.right)
  }

  private def load[T : Decoder](path: Path, default: T): T =
    if (Files.exists(path))
      decode[T](Files.readString(path)).toTry.get
    else
      default

  private implicit def mapDecoder[K : Decoder, V : Decoder]: Decoder[Map[K, V]] =
    Decoder[List[(K, V)]].map(_.toMap)

  private def toOutput(id: Item.ID, pageID: Page.ID, wikiItem: WikiItem): Output = {
    val item = toItem(id, wikiItem)
    val images = wikiItem.images.map(image => toImagePath(id, image) -> image.data)
    ((pageID, wikiItem.infobox.version), item, images)
  }

  private def toItem(id: Item.ID, wikiItem: WikiItem): Item =
    Item(
      id,
      wikiItem.infobox.gameID.asInstanceOf[WikiItem.GameID.Live].raw,
      toName(wikiItem.infobox),
      wikiItem.infobox.examine,
      wikiItem.images.map(image => (image.bin, toImagePath(id, image))),
      wikiItem.infobox.bankable,
      wikiItem.infobox.stackable
    )

  private def toName(item: WikiItem.Infobox): String =
    item.version match {
      case Some(version) => s"${item.wikiName.wikiName} ($version)"
      case None => item.wikiName.wikiName
    }

  private def toImagePath(id: Item.ID, image: WikiItem.Image): Item.Image.Path =
    Item.Image.Path(s"${id.raw}/${image.bin.floor}.${image.fileName.extension}")

  private def outputSink(
    idMapPath: Path,
    itemDataTarget: Path,
    imagesRootTarget: Path
  )(implicit ec: ExecutionContext): Sink[Output, Future[_]] =
    Flow[Output]
      .alsoToMat(
        dataSink[(WikiKey, Item.ID)](idMapPath)
          .contramap { case (wikiKey, item, _) => wikiKey -> item.id }
      )(Keep.right)
      .alsoToMat(
        dataSink[Item](itemDataTarget, StandardOpenOption.CREATE_NEW)
          .contramap { case (_, item, _) => item }
      )(_.zip(_))
      .mapConcat { case (_, _, images) => images.toList }
      .map { case (subPath, data) => Path.of(subPath.raw) -> data }
      // Values taken from https://oldschool.runescape.wiki/w/Items (2022/05/03)
      .toMat(imageSink(imagesRootTarget, targetWidth = 36, targetHeight = 32))(_.zip(_))
}
