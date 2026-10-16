package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{CompleteDiaryTask, CompleteGridTile, CompleteLeagueTask}
import com.leagueplans.ui.model.plan.{ExpTarget, Plan, Repetitions, Step, StopCondition}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.StepErrorFinder.Findings
import com.leagueplans.ui.projection.calculation.loops.{LoopMessages, StopConditions}
import com.leagueplans.ui.projection.calculation.validation.{EffectValidator, RequirementValidator}
import com.leagueplans.ui.projection.model.{LoopRun, StepError, StepTiming}
import com.leagueplans.uicommon.utils.scala.IntOps.{ordinal, withCommas}
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object StepErrorFinder {
  /** @param loops how each pass of each step that repeats until a condition is met played out
    * @param timings when each step happens and how long it takes
    * @param endTime how long the whole plan takes
    */
  final case class Findings(
    errors: Map[Step.ID, List[StepError]],
    loops: Map[Step.ID, List[LoopRun]],
    timings: Map[Step.ID, StepTiming],
    endTime: Duration
  )

  /** Whether a step that the step is part of plays more than one repetition at a time */
  def insideLoop(forest: Forest[Step.ID, Step], loops: Map[Step.ID, Vector[LoopRun]], id: Step.ID): Boolean =
    forest.ancestors(id).exists(repeats(forest, loops, _))

  /** Whether the step plays more than one repetition at a time */
  private def repeats(forest: Forest[Step.ID, Step], loops: Map[Step.ID, Vector[LoopRun]], id: Step.ID): Boolean =
    forest.get(id).exists(_.repetitions match {
      case Repetitions.Fixed(n) => n > 1
      case Repetitions.Until(_, _) => loops.get(id).exists(_.exists(_.reps > 1))
    })
}

/** @param initialPlayer the player before the plan's first step */
final class StepErrorFinder(settings: Plan.Settings, initialPlayer: Player, resolver: EffectResolver, cache: Cache) {
  private val maybeLeague = settings.maybeLeaguePointScoring.map(_.league)
  private type Acc = (Map[Step.ID, List[StepError]], Map[Effect, Step.ID], Map[Step.ID, Int], Player)
  private val looping = StepLooping[Acc](resolver, cache, _._4)

  def findAsync(
    forest: Forest[Step.ID, Step],
    signal: dom.AbortSignal
  ): Future[Option[Findings]] =
    ForestFolder.foldLeftAsync(
      forest,
      (Map.empty[Step.ID, List[StepError]], Map.empty[Effect, Step.ID], Map.empty[Step.ID, Int], initialPlayer),
      signal,
      looping
    ) { case ((errors, completionOwners, processedReps, player), step, reps) =>
      val priorReps = processedReps.getOrElse(step.id, 0)
      val newProcessedReps = processedReps + (step.id -> (priorReps + reps))
      val (finalErrors, finalOwners, finalPlayer) =
        (0 until reps).foldLeft((errors, completionOwners, player)) { case ((errors, owners, repPlayer), repIndex) =>
          val overallReps = priorReps + repIndex + 1
          if (errors.contains(step.id))
            (errors, owners, advancePlayer(repPlayer, step))
          else {
            val (stepErrors, updatedPlayer, updatedOwners) = validateStep(step.id, repPlayer, step, owners)
            val prefixedErrors =
              if (reps > 1 || overallReps > 1) stepErrors.map(e => e.copy(message = s"${overallReps.ordinal} time: ${e.message}"))
              else stepErrors
            val newErrors = if (prefixedErrors.isEmpty) errors else errors + (step.id -> prefixedErrors)
            (newErrors, updatedOwners, updatedPlayer)
          }
        }
      (finalErrors, finalOwners, newProcessedReps, finalPlayer)
    }
    .map(_.map { case ForestFolder.Result((errors, _, _, _), unplaced, places, timings, elapsed) =>
      val loops = unplaced.map((id, runs) => id -> placed(forest, unplaced, runs, places.getOrElse(id, Vector.empty)))
      val loopErrors = loops.flatMap((id, runs) => forest.get(id).map(step => id -> loopProblems(step, runs)))
      val conflicts = forest.nodes.map((id, step) => id -> targetConflicts(step)).filter(_._2.nonEmpty)
      Findings(
        errors = (errors.keySet ++ loopErrors.keySet ++ conflicts.keySet).flatMap(id =>
          Option(loopErrors.getOrElse(id, List.empty) ++ conflicts.getOrElse(id, List.empty) ++ errors.getOrElse(id, List.empty))
            .filter(_.nonEmpty)
            .map(id -> _)
        ).toMap,
        loops = loops.view.mapValues(_.toList).toMap,
        timings = timings.map((id, timing) => id -> timing.copy(insideLoop = StepErrorFinder.insideLoop(forest, unplaced, id))),
        endTime = elapsed.getOrElse(Duration.Zero)
      )
    })


  /** Names where each pass was played, by the repetitions of the steps around it that repeat */
  private def placed(
    forest: Forest[Step.ID, Step],
    loops: Map[Step.ID, Vector[LoopRun]],
    runs: Vector[LoopRun],
    places: Vector[List[(Step.ID, Int)]]
  ): Vector[LoopRun] =
    runs.zipWithIndex.map((run, index) =>
      run.copy(where = places.lift(index).getOrElse(List.empty).collect {
        case (id, rep) if StepErrorFinder.repeats(forest, loops, id) => LoopRun.Place(forest.get(id).fold("")(_.description), rep)
      })
    )

  /** One problem for each different thing that went wrong */
  private def loopProblems(step: Step, runs: Vector[LoopRun]): List[StepError] =
    step.repetitions match {
      case Repetitions.Fixed(_) =>
        List.empty
      case until @ Repetitions.Until(condition, _) =>
        LoopMessages.forPasses(runs.toList, condition, step.description, cache.items)
          .map(problem => StepError(StepError.Source.Repetitions(until), problem.message))
    }

  /** Effects that take a skill to a target the step repeats until, or past it, in one repetition,
    * which stops the step after its first. Only targets that stop the step on their own count. */
  private def targetConflicts(step: Step): List[StepError] =
    step.repetitions match {
      case Repetitions.Fixed(_) =>
        List.empty
      case Repetitions.Until(condition, _) =>
        val targets = StopConditions.sufficientLeaves(condition).collect {
          case StopCondition.SkillReaches(skill, target, _) => (skill, target)
        }
        step.directEffects.underlying.zipWithIndex.flatMap {
          case (effect @ Effect.GainExpToTarget(skill, reached, _), index) =>
            targets.collectFirst {
              case (`skill`, target) if reached.goal.raw >= target.goal.raw =>
                StepError(
                  StepError.Source.Effect(index, effect),
                  s"This reaches ${describe(target)} the first time the step runs, so the step won't repeat"
                )
            }
          case _ =>
            None
        }
    }

  private def describe(target: ExpTarget): String =
    target match {
      case ExpTarget.AtLevel(level) => s"level $level"
      case ExpTarget.AtExp(exp) => s"${(exp.raw / 10).withCommas} xp"
    }

  private def advancePlayer(player: Player, step: Step): Player =
    step.directEffects.underlying.foldLeft(player)(resolver.resolve)

  private def validateStep(
    stepId: Step.ID,
    player: Player,
    step: Step,
    completionOwners: Map[Effect, Step.ID]
  ): (List[StepError], Player, Map[Effect, Step.ID]) = {
    val requirementErrors =
      step.requirements.zipWithIndex.flatMap((requirement, index) =>
        RequirementValidator.validate(requirement)(player, maybeLeague, cache).map(
          StepError(StepError.Source.Requirement(index, requirement), _)
        )
      )
    val (effectErrors, postEffectsPlayer, updatedOwners) =
      step.directEffects.underlying.zipWithIndex.foldLeft((List.empty[StepError], player, completionOwners)) {
        case ((errorAcc, preEffectPlayer, owners), (effect, index)) =>
          val postEffectPlayer = resolver.resolve(preEffectPlayer, effect)
          val errors =
            if (owners.get(effect).contains(stepId)) List.empty
            else EffectValidator.validate(effect)(preEffectPlayer, postEffectPlayer, maybeLeague, cache)
          val newOwners = recordOwnership(effect, stepId, owners)
          (errorAcc ++ errors.map(StepError(StepError.Source.Effect(index, effect), _)), postEffectPlayer, newOwners)
      }
    (requirementErrors ++ effectErrors, postEffectsPlayer, updatedOwners)
  }

  private def recordOwnership(
    effect: Effect,
    stepId: Step.ID,
    owners: Map[Effect, Step.ID]
  ): Map[Effect, Step.ID] =
    effect match {
      case _: CompleteLeagueTask | _: CompleteDiaryTask | _: CompleteGridTile if !owners.contains(effect) =>
        owners + (effect -> stepId)
      case _ => owners
    }
}
