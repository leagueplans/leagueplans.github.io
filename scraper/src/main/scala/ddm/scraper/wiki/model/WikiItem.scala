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

  final case class Infobox(
    gameID: GameID,
    wikiPageID: Page.ID,
    gameName: String,
    wikiName: Page.Name.Other,
    version: Option[String],
    imageBins: NonEmptyList[(Item.Image.Bin, Page.Name.File)],
    examine: String,
    bankable: Item.Bankable,
    stackable: Boolean
  )
}

final case class WikiItem(
  infobox: WikiItem.Infobox,
  images: NonEmptyList[WikiItem.Image]
)
