package ddm.ui.model.plan

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import java.util.UUID

object Step {
  def apply(description: String, directEffects: EffectList): Step =
    Step(UUID.randomUUID(), description, directEffects)

  implicit val encoder: Encoder[Step] =
    Encoder[JsonObject].contramap(step =>
      JsonObject(
        "description" -> step.description.asJson,
        "directEffects" -> step.directEffects.underlying.asJson
      )
    )

  implicit val decoder: Decoder[Step] =
    Decoder[JsonObject].emap(obj =>
      for {
        description <- decodeField[String](obj, "description")
        directEffects <- decodeField[List[Effect]](obj, "directEffects")
      } yield Step(description, EffectList(directEffects))
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Step(
  id: UUID,
  description: String,
  directEffects: EffectList
)
