package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

object StepMappings {
  given Encoder[StepMappings] = Encoder.derived
  given Decoder[StepMappings] = Decoder.derived
}

final case class StepMappings(value: Map[Step.ID, List[Step.ID]]) {
  def update(f: Map[Step.ID, List[Step.ID]] => Map[Step.ID, List[Step.ID]]): StepMappings =
    copy(f(value))
}
