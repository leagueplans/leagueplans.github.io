package ddm.ui.model.player.item

import io.circe.{Decoder, JsonObject}

object Item {
  final case class ID(raw: String)

  implicit val decoder: Decoder[Item] =
    Decoder.decodeJsonObject.emap(obj =>
      for {
        id <- decodeField[String](obj, "id")
        name <- decodeField[String](obj, "name")
        stackable <- decodeField[Boolean](obj, "stackable")
        examine <- decodeField[String](obj, "examine")
      } yield Item(ID(id), name, stackable, examine)
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
