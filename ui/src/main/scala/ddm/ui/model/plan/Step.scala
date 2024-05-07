package ddm.ui.model.plan

import ddm.ui.utils.HasID
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import java.util.UUID

object Step {
  def apply(description: String): Step =
    Step(UUID.randomUUID(), description, EffectList.empty, requirements = List.empty)

  given HasID.Aux[Step, UUID] = HasID[Step, UUID](_.id)
  
  given Encoder[Step] =
    Encoder[JsonObject].contramap(step =>
      JsonObject(
        "description" -> step.description.asJson,
        "directEffects" -> step.directEffects.underlying.asJson,
        "requirements" -> step.requirements.asJson
      )
    )

  given Decoder[Step] =
    Decoder[JsonObject].emap(obj =>
      for {
        description <- decodeField[String](obj, "description")
        directEffects <- decodeField[List[Effect]](obj, "directEffects")
        requirements <- decodeField[List[Requirement]](obj, "requirements")
      } yield Step(UUID.randomUUID(), description, EffectList(directEffects), requirements)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Step(
  id: UUID,
  description: String,
  directEffects: EffectList,
  requirements: List[Requirement]
)
