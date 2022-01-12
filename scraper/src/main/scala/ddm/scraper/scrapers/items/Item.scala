package ddm.scraper.scrapers.items

import io.circe.syntax.EncoderOps
import io.circe.{Encoder, JsonObject}

object Item {
  implicit val encoder: Encoder[Item] =
    Encoder[JsonObject].contramap(item =>
      JsonObject(
        "id" -> item.id.asJson,
        "name" -> item.name.asJson,
        "examine" -> item.examine.asJson,
        "stackable" -> item.stackable.asJson
      )
    )
}

final case class Item(
  id: String,
  name: String,
  stackable: Boolean,
  examine: String
)
