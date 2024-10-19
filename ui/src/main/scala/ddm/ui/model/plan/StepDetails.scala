package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

object StepDetails {
  def apply(description: String): StepDetails =
    StepDetails(
      description,
      EffectList.empty,
      requirements = List.empty
    )
  
  given Encoder[StepDetails] = Encoder.derived
  given Decoder[StepDetails] = Decoder.derived
}

final case class StepDetails(
  description: String,
  directEffects: EffectList,
  requirements: List[Requirement]
)
