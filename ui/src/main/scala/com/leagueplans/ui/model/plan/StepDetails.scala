package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

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
