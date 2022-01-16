package ddm.ui.model.plan

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import java.util.UUID

object Step {
  def apply(description: String, directEffects: Set[Effect]): Step =
    Step(UUID.randomUUID(), description, directEffects)

  implicit val encoder: Encoder[Step] =
    Encoder[JsonObject].contramap(step =>
      JsonObject(
        "description" -> step.description.asJson,
        "directEffects" -> step.directEffects.asJson
      )
    )

  implicit val decoder: Decoder[Step] =
    Decoder[JsonObject].emap(obj =>
      for {
        description <- decodeField[String](obj, "description")
        directEffects <- decodeField[Set[Effect]](obj, "directEffects")
      } yield Step(description, directEffects)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Step(
  id: UUID,
  description: String,
  directEffects: Set[Effect]
)
