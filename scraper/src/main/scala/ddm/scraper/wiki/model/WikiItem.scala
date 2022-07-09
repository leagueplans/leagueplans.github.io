package ddm.scraper.wiki.model

import cats.data.NonEmptyList
import ddm.common.model.Item
import io.circe.{Decoder, Encoder}

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

  object Version {
    implicit val encoder: Encoder[Version] = Encoder[List[String]].contramap(_.raw)
    implicit val decoder: Decoder[Version] = Decoder[List[String]].map(Version.apply)

    implicit val ordering: Ordering[Version] =
      Ordering.by[Version, List[String]](_.raw)(Ordering.Implicits.seqOrdering)
  }

  final case class Version(raw: List[String])

  final case class Infobox(
    gameID: GameID,
    wikiPageID: Page.ID,
    gameName: String,
    wikiName: Page.Name.Other,
    version: Version,
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
