package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.{Repetitions, Step}

/** How each pass of a step that repeats until a condition is met played out.
  *
  * @param repetitions the step's repetitions when the passes were worked out. The passes are found
  *                    in a worker, and lag behind the step while they're recalculated, as its
  *                    problems do.
  */
final case class StepLoop(repetitions: Repetitions, passes: List[LoopRun]) {
  /** Whether the step still repeats as it did when the passes were worked out */
  def isCurrentFor(step: Step): Boolean =
    step.repetitions == repetitions
}

object StepLoop {
  given Encoder[StepLoop] = Encoder.derived
  given Decoder[StepLoop] = Decoder.derived
}
