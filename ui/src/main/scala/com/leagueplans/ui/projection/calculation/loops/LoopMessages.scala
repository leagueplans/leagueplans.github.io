package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.StopCondition
import com.leagueplans.ui.projection.model.LoopRun
import com.leagueplans.uicommon.utils.scala.IntOps.{ordinal, withCommas}

/** The problems a step that repeats until a condition is met can have */
object LoopMessages {
  /** @param where for a step inside others that repeat, where the problem happened, such as
    *              "Fishing trip (2nd time) › Fly fish (13th time)", once for each place
    */
  final case class Problem(message: String, where: List[String])

  /** One problem for each different thing that went wrong across the step's passes.
    *
    * A step inside others that repeat can go wrong on any of their repetitions, so its problems
    * don't name a repetition. They say where they happened instead. A step that isn't names the
    * repetition a problem happened on.
    *
    * @param step the step's description
    */
  def forPasses(runs: List[LoopRun], condition: StopCondition, step: String, items: Item.ID => Item): List[Problem] = {
    val nested = runs.exists(_.where.nonEmpty)
    val found =
      runs.flatMap(run =>
        problems(run, condition, items, nested).map((message, ownRep) =>
          message -> (run.where.map(place => s"${place.step} (${place.rep.ordinal} time)") ++
            ownRep.map(rep => s"$step (${rep.ordinal} time)")).mkString(" › ")
        )
      )
    found.map(_._1).distinct.map(message =>
      Problem(message, if (nested) found.collect { case (`message`, place) => place }.distinct else List.empty)
    )
  }

  /** The problems with a pass, each with the step's own repetition it happened on, if it happened
    * on one */
  private def problems(
    run: LoopRun,
    condition: StopCondition,
    items: Item.ID => Item,
    nested: Boolean
  ): List[(String, Option[Int])] = {
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
    val passed = run.passedOn.map { passed =>
      val passing = capitalise(
        passed.conditions.collect { case leaf: StopConditions.Leaf => StopConditions.describeStoppingBefore(leaf, items) }.mkString(" and ")
      )
      if (nested) (s"$passing before this step stops", Some(passed.rep))
      else (s"$passing the ${passed.rep.ordinal} time this step runs", None)
    }
    outcome.map(_ -> None).toList ++ passed
  }

  private def runs(reps: Int): String =
    if (reps == 1) "runs once" else s"stops after running ${reps.withCommas} times"

  private def capitalise(text: String): String =
    text.headOption.fold(text)(first => s"${first.toUpper}${text.tail}")
}
