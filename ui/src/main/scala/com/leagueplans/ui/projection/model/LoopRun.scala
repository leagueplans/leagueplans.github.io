package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.StopCondition

/** How one pass of a step that repeats until a condition is met played out. A step inside other
  * repeated steps has a pass for each time they play it.
  *
  * @param reps how many repetitions were played
  * @param passedOn the repetitions that took a condition asking to stop before it's passed past its
  *                 threshold, which happens when conditions combined with "and" carry on because
  *                 the others haven't been met
  * @param where for a step inside others that repeat, the repetition of each of those that the
  *              pass was played on, outermost first
  */
final case class LoopRun(
  reps: Int,
  outcome: LoopRun.Outcome,
  passedOn: List[LoopRun.Passed],
  where: List[LoopRun.Place] = List.empty
)

object LoopRun {
  enum Outcome {
    /** Met before the first repetition, so the step isn't played
      *
      * @param conditions the conditions that were met, of those making up the step's condition
      */
    case AlreadyMet(conditions: List[StopCondition])
    /** @param conditions the conditions that were met, of those making up the step's condition */
    case Met(conditions: List[StopCondition])
    /** The next repetition would have passed these conditions, so it isn't played */
    case StoppedBefore(conditions: List[StopCondition])
    /** A repetition brought the condition no closer, so it never would be met */
    case NoProgress
    /** At the rate of the last repetition, the condition would take more repetitions than any step
      * is allowed */
    case TooSlow(estimatedReps: Long)
    /** The step's own limit */
    case ReachedMax(max: Int)
    /** The limit every step that repeats until a condition is met has */
    case ReachedCap(cap: Int)
  }

  object Outcome {
    given Encoder[Outcome] = Encoder.derived
    given Decoder[Outcome] = Decoder.derived
  }

  final case class Passed(rep: Int, conditions: List[StopCondition])

  object Passed {
    given Encoder[Passed] = Encoder.derived
    given Decoder[Passed] = Decoder.derived
  }

  /** A repetition of a step, named by its description */
  final case class Place(step: String, rep: Int)

  object Place {
    given Encoder[Place] = Encoder.derived
    given Decoder[Place] = Decoder.derived
  }

  given Encoder[LoopRun] = Encoder.derived
  given Decoder[LoopRun] = Decoder.derived
}
