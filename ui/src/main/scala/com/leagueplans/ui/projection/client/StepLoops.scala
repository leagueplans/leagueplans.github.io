package com.leagueplans.ui.projection.client

import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.client.StepLoops.Known
import com.leagueplans.ui.projection.model.StepLoop
import com.raquo.airstream.core.Signal

object StepLoops {
  /** A step's passes as last worked out, with the repetitions they were worked out for
    *
    * @param current whether they were worked out for how the step repeats now. Until the worker
    *                catches up with a change to the step, its last passes are kept, rather than
    *                blanked, so that what's shown doesn't flicker.
    */
  final case class Known(loop: StepLoop, current: Boolean)
}

/** How each pass of each step that repeats until a condition is met played out, as last worked out
  * by the projection worker. The passes catch up with the plan a moment after it changes. */
final class StepLoops(val all: Signal[Map[Step.ID, StepLoop]]) {
  /** The step's passes as last worked out, or None if they never have been while it repeated
    * until a condition */
  def get(step: Signal[Step]): Signal[Option[Known]] =
    Signal.combine(step, all).map((step, all) =>
      all.get(step.id).map(loop => Known(loop, loop.isCurrentFor(step)))
    ).distinct
}
