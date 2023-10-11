package ddm.scraper.dumper

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.model.{InfoboxVersion, Page, WikiItem}

import java.nio.file.Path
import java.util.UUID
import scala.concurrent.Future

object ItemDumper {
  private type WikiKey = (Page.ID, InfoboxVersion)
  private type Output = (WikiKey, Item, NonEmptyList[(Item.Image.Path, Array[Byte])])

  def dump(
    existingIDMap: Map[WikiKey, Item.ID],
    imagesDirectory: Path,
    idMapWriter: ActorRef[Cache.Message[(WikiKey, Item.ID)]],
    itemWriter: ActorRef[Cache.Message[Item]]
  ): Sink[(Page, WikiItem), Future[_]] =
    Flow[(Page, WikiItem)]
      .map { case (page, wikiItem) =>
        val wikiKey = (page.id, wikiItem.infoboxes.version)
        val itemID = existingIDMap.getOrElse(wikiKey, Item.ID(UUID.randomUUID().toString))
        toOutput(itemID, page.id, wikiItem)
      }
      .toMat(outputSink(idMapWriter, itemWriter, imagesDirectory))(Keep.right)

  private def toOutput(id: Item.ID, pageID: Page.ID, wikiItem: WikiItem): Output = {
    val item = toItem(id, wikiItem)
    val images = wikiItem.images.map(image => toImagePath(id, image) -> image.data)
    ((pageID, wikiItem.infoboxes.version), item, images)
  }

  private def toItem(id: Item.ID, wikiItem: WikiItem): Item =
    Item(
      id,
      toName(wikiItem.infoboxes),
      wikiItem.infoboxes.item.examine,
      wikiItem.images.map(image => (image.bin, toImagePath(id, image))),
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

  private def toImagePath(id: Item.ID, image: WikiItem.Image): Item.Image.Path =
    Item.Image.Path(s"${id.raw}/${image.bin.floor}.${image.fileName.extension}")

  private def outputSink(
    idMapWriter: ActorRef[Cache.Message[(WikiKey, Item.ID)]],
    itemWriter: ActorRef[Cache.Message[Item]],
    imagesDirectory: Path
  ): Sink[Output, Future[_]] =
    Flow[Output]
      .alsoTo(dataSink(idMapWriter).contramap { case (wikiKey, item, _) => wikiKey -> item.id })
      .alsoTo(dataSink(itemWriter).contramap { case (_, item, _) => item })
      .mapConcat { case (_, _, images) => images.toList }
      .map { case (subPath, data) => Path.of(subPath.raw) -> data }
      // Values taken from https://oldschool.runescape.wiki/w/Items (2022/05/03)
      .toMat(imageSink(imagesDirectory, targetWidth = 36, targetHeight = 32))(Keep.right)
}
