package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.utils.HasID

import java.util.UUID

object Step {
  def apply(description: String): Step =
    Step(ID.generate(), StepDetails(description))

  opaque type ID <: String = String

  object ID {
    def generate(): ID =
      UUID.randomUUID().toString

    def fromString(stepID: String): ID =
      stepID

    given Encoder[ID] = Encoder.stringEncoder
    given Decoder[ID] = Decoder.stringDecoder
  }
  
  given HasID.Aux[Step, ID] = HasID[Step, ID](_.id)
  
  given Encoder[Step] = Encoder.derived
  given Decoder[Step] = Decoder.derived
}

final case class Step(id: Step.ID, details: StepDetails) {
  export details.{description, directEffects, requirements}
  
  def deepCopy(
    id: Step.ID = id,
    description: String = details.description,
    directEffects: EffectList = details.directEffects,
    requirements: List[Requirement] = details.requirements
  ): Step =
    Step(id, StepDetails(description, directEffects, requirements))
}
