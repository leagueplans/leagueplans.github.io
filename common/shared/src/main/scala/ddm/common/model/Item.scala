package ddm.common.model

import cats.data.NonEmptyList
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import java.util.UUID

object Item {
  final case class ID(raw: UUID)

  object Image {
    final case class Bin(floor: Int)
    final case class Path(raw: String)

    implicit val binEncoder: Encoder[Bin] = Encoder[Int].contramap(_.floor)
    implicit val binDecoder: Decoder[Bin] = Decoder[Int].map(Bin)
    implicit val pathEncoder: Encoder[Path] = Encoder[String].contramap(_.raw)
    implicit val pathDecoder: Decoder[Path] = Decoder[String].map(Path)
  }

  sealed trait Bankable

  object Bankable {
    final case class Yes(stacks: Boolean) extends Bankable
    case object No extends Bankable

    implicit val yesEncoder: Encoder[Yes] = Encoder[Boolean].contramap(_.stacks)
    implicit val yesDecoder: Decoder[Yes] = Decoder[Boolean].map(Yes)
    implicit val noCodec: Codec[No.type] = deriveCodec
    implicit val bankableCodec: Codec[Bankable] = deriveCodec
  }

  implicit val idEncoder: Encoder[ID] = Encoder[UUID].contramap(_.raw)
  implicit val idDecoder: Decoder[ID] = Decoder[UUID].map(ID)
  implicit val codec: Codec[Item] = deriveCodec
}

final case class Item(
  id: Item.ID,
  gameID: Int, //TODO Remove
  name: String,
  examine: String,
  images: NonEmptyList[(Item.Image.Bin, Item.Image.Path)],
  bankable: Item.Bankable,
  stackable: Boolean
) {
  def imageFor(count: Int): Item.Image.Path = {
    val (_, path) =
      images
        .toList
        .takeWhile { case (bin, _) => bin.floor <= count }
        .lastOption
        .getOrElse(images.head)

    path
  }
}
