package ddm.common.model

import cats.data.NonEmptyList
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object Item {
  final case class ID(raw: String) extends AnyVal

  object Image {
    final case class Bin(floor: Int) extends AnyVal
    final case class Path(raw: String) extends AnyVal

    given Encoder[Bin] = Encoder[Int].contramap(_.floor)
    given Decoder[Bin] = Decoder[Int].map(Bin.apply)
    given Encoder[Path] = Encoder[String].contramap(_.raw)
    given Decoder[Path] = Decoder[String].map(Path.apply)
  }

  enum Bankable {
    case Yes(stacks: Boolean)
    case No
  }

  object Bankable {
    given Encoder[Yes] = Encoder[Boolean].contramap(_.stacks)
    given Decoder[Yes] = Decoder[Boolean].map(Yes.apply)
    given Codec[No.type] = deriveCodec
    given Codec[Bankable] = deriveCodec
  }

  given Ordering[ID] = Ordering.by(_.raw)
  given Encoder[ID] = Encoder[String].contramap(_.raw)
  given Decoder[ID] = Decoder[String].map(ID.apply)

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
