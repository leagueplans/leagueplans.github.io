package ddm.common.model

import cats.data.NonEmptyList
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder as JsonDecoder, Encoder as JsonEncoder}

object Item {
  final case class ID(raw: String) extends AnyVal

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

  given Ordering[ID] = Ordering.by(_.raw)
  given JsonEncoder[ID] = JsonEncoder[String].contramap(_.raw)
  given JsonDecoder[ID] = JsonDecoder[String].map(ID.apply)
  given Encoder[ID] = Encoder.stringEncoder.contramap(_.raw)
  given Decoder[ID] = Decoder.stringDecoder.map(ID.apply)

  given Ordering[Item] = Ordering.by(item => (item.name, item.examine, item.id))
  given Codec[Item] = deriveCodec
}

final case class Item(
  id: Item.ID,
  name: String,
  examine: String,
  images: NonEmptyList[(Item.Image.Bin, Item.Image.Path)],
  bankable: Item.Bankable,
  stackable: Boolean,
  noteable: Boolean,
  equipmentType: Option[EquipmentType]
) {
  def imageFor(count: Int): Item.Image.Path = {
    val (_, path) =
      images
        .toList
        .takeWhile((bin, _) => bin.floor <= count)
        .lastOption
        .getOrElse(images.head)

    path
  }
}
