package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step

object StepUpdates {
  given Encoder[StepUpdates] = Encoder.derived
  given Decoder[StepUpdates] = Decoder.derived
}

/** Updates to a plan's steps that were made together, in the order they were made */
final case class StepUpdates(updates: List[Forest.Update[Step.ID, Step]])
