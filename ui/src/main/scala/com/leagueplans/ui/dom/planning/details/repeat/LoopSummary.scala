package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Repetitions, StopCondition}
import com.leagueplans.ui.projection.calculation.loops.{LoopMessages, StopConditions}
import com.leagueplans.ui.projection.model.{LoopRun, StepLoop}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** Words for how a step that repeats until a condition is met played out, for its badge in the plan
  * and the result under its conditions */
object LoopSummary {
  enum Result {
    /** The loop ended as asked, such as "Stops after running 412 times" */
    case Stops(text: String)
    /** What went wrong, as the step's problems say it */
    case Problems(messages: List[String])
  }

  /** How many times the step ran, such as "412", or "21–27" when that differs between the times the plan reaches it */
  def count(passes: List[LoopRun]): String =
    passes.map(_.reps) match {
      case Nil => "0"
      case reps if reps.min == reps.max => reps.head.withCommas
      case reps => s"${reps.min.withCommas}–${reps.max.withCommas}"
    }

  /** Such as "Runs 412 times, until Hunter reaches level 60" */
  def tooltip(condition: StopCondition, passes: Option[List[LoopRun]], items: Item.ID => Item): String = {
    val until = StopConditions.describe(condition, items)
    passes match {
      case None => s"Repeats until $until. Working out how many times that takes…"
      case Some(passes @ (_ :: _ :: _)) => s"Runs ${ran(passes)} each time the plan reaches it, until $until"
      case Some(passes) =>
        passes.headOption.fold(0)(_.reps) match {
          case 0 => s"Doesn't run, though it repeats until $until"
          case reps => s"Runs ${times(reps)}, until $until"
        }
    }
  }

  def result(loop: StepLoop, items: Item.ID => Item): Result =
    loop.repetitions match {
      case Repetitions.Fixed(_) =>
        Result.Stops(s"Stops after running ${times(loop.passes.map(_.reps).sum)}")
      case Repetitions.Until(condition, _) =>
        LoopMessages.forPasses(loop.passes, condition, items) match {
          case Nil => Result.Stops(stops(loop.passes, condition, items))
          case problems => Result.Problems(problems)
        }
    }

  private def stops(passes: List[LoopRun], condition: StopCondition, items: Item.ID => Item): String =
    passes match {
      case List(run) if run.reps == 0 =>
        "Doesn't run"
      case List(run) =>
        run.outcome match {
          case LoopRun.Outcome.Met(met) if StopConditions.leaves(condition).size > 1 && StopConditions.sufficientLeaves(condition).nonEmpty =>
            // With any of several, the result names the ones that stopped it
            val named = met.collect { case leaf: StopConditions.Leaf => StopConditions.describeLeaf(leaf, items) }
            val first = if (named.size == 1) " first" else ""
            s"Stops after running ${times(run.reps)}: ${named.mkString(" and ")}$first"
          case LoopRun.Outcome.StoppedBefore(passing) =>
            val named = passing.collect { case leaf: StopConditions.Leaf => StopConditions.describeStoppingBefore(leaf, items) }
            s"Stops after running ${times(run.reps)}, before ${named.mkString(" and ")}"
          case LoopRun.Outcome.ReachedMax(max) =>
            s"Stops at its limit of ${max.withCommas} ${if (max == 1) "time" else "times"}"
          case _ =>
            s"Stops after running ${times(run.reps)}"
        }
      case Nil =>
        "Doesn't run"
      case _ if passes.size <= 4 && passes.forall(_.reps == 1) =>
        s"Runs once, on the ${passes.size} times the plan reaches it"
      case _ if passes.size <= 4 =>
        val counts = passes.map(_.reps.withCommas)
        s"Runs ${counts.init.mkString(", ")} and ${counts.last} times, on the ${passes.size} times the plan reaches it"
      case _ =>
        s"Runs ${ran(passes)}, on each of the ${passes.size.withCommas} times the plan reaches it"
    }

  /** Such as "412 times", or "once" when every time the plan reaches the step it runs once */
  private def ran(passes: List[LoopRun]): String =
    if (count(passes) == "1") "once" else s"${count(passes)} times"

  private def times(n: Int): String =
    if (n == 1) "once" else s"${n.withCommas} times"
}
