package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.StopCondition
import com.leagueplans.ui.projection.model.LoopRun
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The problems a pass of a step that repeats until a condition is met can have */
object LoopMessages {
  /** One problem for each different thing that went wrong, naming the pass it first went wrong on
    * if the step is played more than once */
  def forPasses(runs: List[LoopRun], condition: StopCondition, items: Item.ID => Item): List[String] =
    runs.zipWithIndex
      .flatMap((run, pass) => problems(run, condition, items).map(_ -> pass))
      .distinctBy(_._1)
      .map((message, pass) => if (runs.size > 1) s"Pass ${pass + 1}: $message" else message)

  def problems(run: LoopRun, condition: StopCondition, items: Item.ID => Item): List[String] = {
    val outcome = run.outcome match {
      case LoopRun.Outcome.AlreadyMet(met) =>
        val leaves = met.collect { case leaf: StopConditions.Leaf => StopConditions.describeAlreadyMet(leaf, items) }
        Some(s"${capitalise(if (leaves.isEmpty) "its condition is already met" else leaves.mkString(" and "))}, so this step doesn't run")
      case LoopRun.Outcome.NoProgress =>
        val approach = StopConditions.leaves(condition) match {
          case List(leaf) => StopConditions.describeApproach(leaf, items)
          case _ => "this step closer to stopping"
        }
        Some(s"Running this step again doesn't bring $approach")
      case LoopRun.Outcome.TooSlow(estimate) =>
        Some(s"At this rate, this step would need to run about ${String.format("%,d", estimate)} times, so it ${runs(run.reps)}")
      case LoopRun.Outcome.ReachedCap(cap) =>
        Some(s"This step stops at the limit of ${cap.withCommas} times")
      case LoopRun.Outcome.Met(_) | LoopRun.Outcome.ReachedMax(_) | LoopRun.Outcome.StoppedBefore(_) =>
        None
    }
    val passed = run.passedOn.map(passed =>
      s"Rep ${passed.rep.withCommas} takes ${passed.conditions.collect { case leaf: StopConditions.Leaf => StopConditions.describePassing(leaf, items) }.mkString(" and ")}"
    )
    outcome.toList ++ passed
  }

  private def runs(reps: Int): String =
    if (reps == 1) "runs once" else s"stops after running ${reps.withCommas} times"

  private def capitalise(text: String): String =
    text.headOption.fold(text)(first => s"${first.toUpper}${text.tail}")
}
