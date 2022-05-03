package ddm.scraper.wiki.model

import cats.data.NonEmptyList
import ddm.common.model.Item
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object WikiItem {
  sealed trait GameID

  object GameID {
    //TODO Remove the need for encoding game IDs?
    implicit val betaCodec: Codec[Beta] = deriveCodec
    implicit val historicCodec: Codec[Historic] = deriveCodec
    implicit val liveEncoder: Encoder[Live] = Encoder[Int].contramap(_.raw)
    implicit val liveDecoder: Decoder[Live] = Decoder[Int].map(Live)
    implicit val gameIDCodec: Codec[GameID] = deriveCodec

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
