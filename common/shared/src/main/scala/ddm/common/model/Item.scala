package ddm.common.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

object Item {
  object ID {
    implicit val encoder: Encoder[ID] = Encoder[String].contramap(_.raw)
    implicit val decoder: Decoder[ID] = Decoder[String].map(ID.apply)
  }

  final case class ID(raw: String)

  implicit val codec: Codec[Item] = deriveCodec[Item]
}

final case class Item(
  id: Item.ID,
  name: String,
  stackable: Boolean,
  examine: String
)
