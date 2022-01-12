package ddm.ui.model.player.item

import io.circe.{Decoder, Encoder, JsonObject}

object Item {
  object ID {
    implicit val encoder: Encoder[ID] = Encoder[String].contramap(_.raw)
    implicit val decoder: Decoder[ID] = Decoder[String].map(ID.apply)
  }

  final case class ID(raw: String)

  implicit val decoder: Decoder[Item] =
    Decoder[JsonObject].emap(obj =>
      for {
        id <- decodeField[ID](obj, "id")
        name <- decodeField[String](obj, "name")
        stackable <- decodeField[Boolean](obj, "stackable")
        examine <- decodeField[String](obj, "examine")
      } yield Item(id, name, stackable, examine)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Item(
  id: Item.ID,
  name: String,
  stackable: Boolean,
  examine: String
)
