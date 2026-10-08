package com.leagueplans.common.model

import cats.data.NonEmptyList
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder as JsonDecoder, Encoder as JsonEncoder}

object Item {
  opaque type ID <: Int = Int
  
  object ID {
    inline def apply(id: Int): ID = id
    
    given Ordering[ID] = Ordering.Int
    given JsonEncoder[ID] = JsonEncoder.encodeInt
    given JsonDecoder[ID] = JsonDecoder.decodeInt
    given Encoder[ID] = Encoder.unsignedIntEncoder
    given Decoder[ID] = Decoder.unsignedIntDecoder
  }

  object Image {
    final case class Bin(floor: Int) extends AnyVal
    final case class Path(raw: String) extends AnyVal

    given JsonEncoder[Bin] = JsonEncoder[Int].contramap(_.floor)
    given JsonDecoder[Bin] = JsonDecoder[Int].map(Bin.apply)
    given JsonEncoder[Path] = JsonEncoder[String].contramap(_.raw)
    given JsonDecoder[Path] = JsonDecoder[String].map(Path.apply)
  }

  enum Bankable {
    case Yes(stacks: Boolean)
    case No
  }

  object Bankable {
    given JsonEncoder[Yes] = JsonEncoder[Boolean].contramap(_.stacks)
    given JsonDecoder[Yes] = JsonDecoder[Boolean].map(Yes.apply)
    given Codec[No.type] = deriveCodec
    given Codec[Bankable] = deriveCodec
  }

  /** The name with the infobox version in brackets, such as "Coins (Mage Training Arena)". The
    * versions on a page differ, so this tells apart items that share a page. */
  def fullName(name: String, version: List[String]): String =
    if (version.isEmpty) name else s"$name (${version.mkString(", ")})"

  given Ordering[Item] = Ordering.by(item => (item.fullName, item.examine, item.id))
  given Codec[Item] = deriveCodec
}

final case class Item(
  id: Item.ID,
  gameID: Option[Int],
  /** The name of the item's wiki page, which items on the same page share. See [[fullName]]. */
  name: String,
  examine: String,
  images: NonEmptyList[(Item.Image.Bin, Item.Image.Path)],
  bankable: Item.Bankable,
  stackable: Boolean,
  noteable: Boolean,
  equipmentType: Option[EquipmentType],
  /** The wiki infobox the item was scraped from */
  infobox: InfoboxKey
) {
  /** The name, with the infobox version that tells the item apart from others on its page */
  def fullName: String =
    Item.fullName(name, infobox.version)

  def imageFor(quantity: Int): Item.Image.Path = {
    val (_, path) =
      images
        .toList
        .takeWhile((bin, _) => bin.floor <= quantity)
        .lastOption
        .getOrElse(images.head)

    path
  }
}
