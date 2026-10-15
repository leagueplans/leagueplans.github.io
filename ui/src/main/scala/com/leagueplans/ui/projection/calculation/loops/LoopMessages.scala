package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.StopCondition
import com.leagueplans.ui.projection.model.LoopRun
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The problems a pass of a step that repeats until a condition is met can have */
object LoopMessages {
  def problems(run: LoopRun, condition: StopCondition, items: Item.ID => Item): List[String] = {
    val until = StopConditions.describe(condition, items)
    val outcome = run.outcome match {
      case LoopRun.Outcome.AlreadyMet =>
        Some(s"This step repeats until $until, which is already true when it's reached, so it doesn't run")
      case LoopRun.Outcome.NoProgress =>
        Some(s"A repetition doesn't bring this step any closer to stopping, until $until, so it ${runs(run.reps)}")
      case LoopRun.Outcome.TooSlow(estimate) =>
        Some(s"At this rate, it would take about ${String.format("%,d", estimate)} repetitions until $until, so this step ${runs(run.reps)}")
      case LoopRun.Outcome.ReachedCap(cap) =>
        Some(s"This step stops at the limit of ${cap.withCommas} repetitions, before $until")
      case LoopRun.Outcome.Met | LoopRun.Outcome.ReachedMax(_) | LoopRun.Outcome.StoppedBefore(_) =>
        None
    }
    val passed = run.passedOn.map(passed =>
      s"Rep ${passed.rep.withCommas} takes ${passed.conditions.collect { case leaf: StopConditions.Leaf => StopConditions.describePassing(leaf, items) }.mkString(" and ")}, which it should stop before"
    )
    outcome.toList ++ passed
  }

  private def runs(reps: Int): String =
    if (reps == 1) "runs once" else s"stops after ${reps.withCommas} repetitions"
}
