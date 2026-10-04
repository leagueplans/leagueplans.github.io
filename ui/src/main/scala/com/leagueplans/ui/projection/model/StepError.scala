package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.{Step, Effect as StepEffect, Requirement as StepRequirement}

object StepError {
  /** The effect or requirement on the step that the problem lies with.
    *
    * Both its position and its value are kept. The position is what identifies it, since a step
    * can hold the same effect twice, and the checks can treat the two differently: completing a
    * quest is fine the first time, and a problem the second. The value is there because the
    * problems are found in a worker, and lag behind the step while they're recalculated. Until
    * they catch up, a problem only applies while its position still holds its value, so a stale
    * problem is hidden rather than shown against whatever has moved into that position.
    */
  enum Source {
    case Effect(index: Int, effect: StepEffect)
    case Requirement(index: Int, requirement: StepRequirement)

    /** Whether the step still holds the effect or requirement at the same position */
    def isCurrentFor(step: Step): Boolean =
      this match {
        case Effect(index, effect) => step.directEffects.underlying.lift(index).contains(effect)
        case Requirement(index, requirement) => step.requirements.lift(index).contains(requirement)
      }
  }

  object Source {
    given Encoder[Source] = Encoder.derived
    given Decoder[Source] = Decoder.derived
  }

  given Encoder[StepError] = Encoder.derived
  given Decoder[StepError] = Decoder.derived
}

final case class StepError(source: StepError.Source, message: String)
