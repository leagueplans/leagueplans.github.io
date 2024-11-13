package ddm.scraper.dumper

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Keep, Sink}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.model.WikiItem.GameID
import ddm.scraper.wiki.model.{InfoboxVersion, Page, WikiItem}

import java.nio.file.Path
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.Future

object ItemDumper {
  private type WikiKey = (Page.ID, InfoboxVersion)
  private type Output = (WikiKey, Item, NonEmptyList[(Item.Image.Path, Array[Byte])])

  def dump(
    existingIDMap: Map[WikiKey, Item.ID],
    imagesDirectory: Path,
    idMapWriter: ActorRef[Cache.Message[(WikiKey, Item.ID)]],
    itemWriter: ActorRef[Cache.Message[Item]]
  ): Sink[(Page, WikiItem), Future[?]] =
    Flow[(Page, WikiItem)]
      .statefulMapConcat { () => 
        val allocatedIDs = existingIDMap.values.to[mutable.Set[Int]](mutable.Set)
        var nextPotentialID = 0
        
        (page, wikiItem) => {
          val wikiKey = (page.id, wikiItem.infoboxes.version)
          val itemID = existingIDMap.get(wikiKey) match {
            case Some(id) => id
            case None =>
              val newID = findNextFreeID(nextPotentialID, allocatedIDs)
              allocatedIDs += newID
              nextPotentialID = newID + 1
              newID
          }
          List(toOutput(itemID, page.id, wikiItem))
        }
      }
      .toMat(outputSink(idMapWriter, itemWriter, imagesDirectory))(Keep.right)
    
  @tailrec
  private def findNextFreeID(potentialID: Int, allocatedIDs: mutable.Set[Int]): Item.ID =
    if (allocatedIDs.contains(potentialID))
      findNextFreeID(potentialID + 1, allocatedIDs)
    else
      Item.ID(potentialID)

  private def toOutput(id: Item.ID, pageID: Page.ID, wikiItem: WikiItem): Output = {
    val item = toItem(id, wikiItem)
    val images = wikiItem.images.map(image => toImagePath(id, image) -> image.data)
    ((pageID, wikiItem.infoboxes.version), item, images)
  }

  private def toItem(id: Item.ID, wikiItem: WikiItem): Item =
    Item(
      id,
      toLiveID(wikiItem.infoboxes.item.id),
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

  private def toLiveID(id: WikiItem.GameID): Option[Int] =
    id match {
      case GameID.Live(raw) => Some(raw)
      case _ => None
    }

  private def toImagePath(id: Item.ID, image: WikiItem.Image): Item.Image.Path =
    Item.Image.Path(s"$id/${image.bin.floor}.${image.fileName.extension}")

  private def outputSink(
    idMapWriter: ActorRef[Cache.Message[(WikiKey, Item.ID)]],
    itemWriter: ActorRef[Cache.Message[Item]],
    imagesDirectory: Path
  ): Sink[Output, Future[?]] =
    Flow[Output]
      .alsoTo(dataSink(idMapWriter).contramap((wikiKey, item, _) => wikiKey -> item.id))
      .alsoTo(dataSink(itemWriter).contramap((_, item, _) => item))
      .mapConcat((_, _, images) => images.toList)
      .map((subPath, data) => Path.of(subPath.raw) -> data)
      .toMat(imageSink(imagesDirectory))(Keep.right)
}
