package ddm.scraper.dumper

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.model.WikiItem.GameID
import ddm.scraper.wiki.model.{Page, WikiItem}

import java.nio.file.Path
import java.util.UUID
import scala.concurrent.Future

object ItemDumper {
  private type WikiKey = (Page.ID, Option[String])
  private type Output = (WikiKey, Item, NonEmptyList[(Item.Image.Path, Array[Byte])])

  def dump(
    existingIDMap: Map[WikiKey, Item.ID],
    imagesDirectory: Path,
    idMapWriter: ActorRef[Cache.Message[(WikiKey, Item.ID)]],
    itemWriter: ActorRef[Cache.Message[Item]]
  ): Sink[(Page, WikiItem), Future[_]] =
    Flow[(Page, WikiItem)]
      .filter { case (_, wikiItem) => //TODO Remove
        wikiItem.infobox.gameID match {
          case _: GameID.Beta => false
          case _: GameID.Historic => false
          case _: GameID.Live => true
        }
      }
      .map { case (page, wikiItem) =>
        val wikiKey = (page.id, wikiItem.infobox.version)
        val itemID = existingIDMap.getOrElse(wikiKey, Item.ID(UUID.randomUUID()))
        toOutput(itemID, page.id, wikiItem)
      }
      .toMat(outputSink(idMapWriter, itemWriter, imagesDirectory))(Keep.right)

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
