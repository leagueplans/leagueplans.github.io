package ddm.scraper.wiki.model

import cats.data.NonEmptyList
import ddm.common.model.Item

object WikiItem {
  sealed trait GameID

  object GameID {
    final case class Beta(raw: Int) extends GameID
    final case class Historic(raw: Int) extends GameID
    final case class Live(raw: Int) extends GameID
  }

  final case class Image(
    bin: Item.Image.Bin,
    fileName: Page.Name.File,
    data: Array[Byte]
  )

  final case class Infoboxes(
    pageID: Page.ID,
    pageName: Page.Name.Other,
    version: InfoboxVersion,
    item: ItemInfobox,
    maybeBonuses: Option[BonusesInfobox]
  )
}

final case class WikiItem(
  infoboxes: WikiItem.Infoboxes,
  images: NonEmptyList[WikiItem.Image]
)
