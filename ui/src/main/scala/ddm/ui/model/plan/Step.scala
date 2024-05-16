package ddm.ui.model.plan

import ddm.ui.utils.HasID
import io.circe.syntax.EncoderOps
import io.circe.generic.semiauto.deriveCodec
import io.circe.*

import java.util.UUID

object Step {
  def apply(description: String): Step =
    Step(ID.generate(), description, EffectList.empty, requirements = List.empty)

  opaque type ID <: String = String

  object ID {
    def generate(): ID =
      UUID.randomUUID().toString

    def fromString(stepID: String): ID =
      stepID

    given Encoder[ID] = Encoder.encodeString
    given Decoder[ID] = Decoder.decodeString

    given KeyEncoder[ID] = KeyEncoder.encodeKeyString
    given KeyDecoder[ID] = KeyDecoder.decodeKeyString
  }
  
  given HasID.Aux[Step, ID] = HasID[Step, ID](_.id)
  
  val comprehensiveCodec: Codec[Step] = deriveCodec[Step]
  
  val minimisedEncoder: Encoder[Step] =
    Encoder[JsonObject].contramap(step =>
      JsonObject(
        "description" -> step.description.asJson,
        "directEffects" -> step.directEffects.underlying.asJson,
        "requirements" -> step.requirements.asJson
      )
    )

  def minimisedDecoder(generateID: () => ID): Decoder[Step] =
    Decoder[JsonObject].emap(obj =>
      for {
        description <- decodeField[String](obj, "description")
        directEffects <- decodeField[List[Effect]](obj, "directEffects")
        requirements <- decodeField[List[Requirement]](obj, "requirements")
      } yield Step(generateID(), description, EffectList(directEffects), requirements)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Step(
  id: Step.ID,
  description: String,
  directEffects: EffectList,
  requirements: List[Requirement]
)
