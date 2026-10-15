package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{CompleteDiaryTask, CompleteGridTile, CompleteLeagueTask}
import com.leagueplans.ui.model.plan.{Plan, Repetitions, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.StepErrorFinder.Findings
import com.leagueplans.ui.projection.calculation.loops.LoopMessages
import com.leagueplans.ui.projection.calculation.validation.{EffectValidator, RequirementValidator}
import com.leagueplans.ui.projection.model.{LoopRun, StepError, StepTiming}
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
    forest.ancestors(id).exists(ancestor =>
      forest.get(ancestor).exists(_.repetitions match {
        case Repetitions.Fixed(n) => n > 1
        case Repetitions.Until(_, _) => loops.get(ancestor).exists(_.exists(_.reps > 1))
      })
    )
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
              if (reps > 1 || overallReps > 1) stepErrors.map(e => e.copy(message = s"Rep $overallReps: ${e.message}"))
              else stepErrors
            val newErrors = if (prefixedErrors.isEmpty) errors else errors + (step.id -> prefixedErrors)
            (newErrors, updatedOwners, updatedPlayer)
          }
        }
      (finalErrors, finalOwners, newProcessedReps, finalPlayer)
    }
    .map(_.map { case ForestFolder.Result((errors, _, _, _), loops, timings, elapsed) =>
      val loopErrors = loops.flatMap((id, runs) => forest.get(id).map(step => id -> loopProblems(step, runs)))
      Findings(
        errors = (errors.keySet ++ loopErrors.keySet).flatMap(id =>
          Option((loopErrors.getOrElse(id, List.empty) ++ errors.getOrElse(id, List.empty)))
            .filter(_.nonEmpty)
            .map(id -> _)
        ).toMap,
        loops = loops.view.mapValues(_.toList).toMap,
        timings = timings.map((id, timing) => id -> timing.copy(insideLoop = StepErrorFinder.insideLoop(forest, loops, id))),
        endTime = elapsed.getOrElse(Duration.Zero)
      )
    })


  /** One problem for each different thing that went wrong, naming the pass it first went wrong on
    * if the step is played more than once */
  private def loopProblems(step: Step, runs: Vector[LoopRun]): List[StepError] =
    step.repetitions match {
      case Repetitions.Fixed(_) =>
        List.empty
      case until @ Repetitions.Until(condition, _) =>
        runs.zipWithIndex
          .flatMap((run, pass) => LoopMessages.problems(run, condition, cache.items).map(_ -> pass))
          .distinctBy(_._1)
          .map((message, pass) =>
            StepError(StepError.Source.Repetitions(until), if (runs.size > 1) s"Pass ${pass + 1}: $message" else message)
          )
          .toList
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
