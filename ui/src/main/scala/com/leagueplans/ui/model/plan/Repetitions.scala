package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

// Saved plans refer to these cases by their position, so new cases go at the end

/** How many times a step, with its substeps, is played */
enum Repetitions {
  case Fixed(count: Int)

  /** Played again until the condition is met, checked after each whole repetition. How many
    * repetitions that takes isn't stored. It's worked out each time the plan is replayed.
    *
    * @param max at most this many repetitions, as well as the limit every such step has
    */
  case Until(condition: StopCondition, max: Option[Int])

  /** The count for a fixed number of repetitions, and one for a step that repeats until a
    * condition is met, for readers that have yet to be given the count the plan works out.
    */
  // TODO: remove once every reader has the worked out count
  def assumed: Int =
    this match {
      case Fixed(count) => count
      case Until(_, _) => 1
    }
}

object Repetitions {
  val once: Repetitions = Fixed(1)

  /** The most repetitions a step that repeats until a condition is met can play */
  val cap: Int = 100000

  given Encoder[Repetitions] = Encoder.derived
  given Decoder[Repetitions] = Decoder.derived
}
