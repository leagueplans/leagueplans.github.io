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
  */
final case class LoopRun(
  reps: Int,
  outcome: LoopRun.Outcome,
  passedOn: List[LoopRun.Passed]
)

object LoopRun {
  enum Outcome {
    /** Met before the first repetition, so the step isn't played */
    case AlreadyMet
    case Met
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

  given Encoder[LoopRun] = Encoder.derived
  given Decoder[LoopRun] = Decoder.derived
}
