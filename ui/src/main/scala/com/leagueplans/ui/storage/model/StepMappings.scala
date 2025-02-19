package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

object StepMappings {
  given Encoder[StepMappings] = Encoder.derived
  given Decoder[StepMappings] = Decoder.derived
}

final case class StepMappings(toChildren: Map[Step.ID, List[Step.ID]], roots: List[Step.ID])
