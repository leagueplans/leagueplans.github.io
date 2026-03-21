package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{CompleteDiaryTask, CompleteGridTile, CompleteLeagueTask}
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.calculation.validation.{EffectValidator, RequirementValidator}
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

final class StepErrorFinder(settings: Plan.Settings, resolver: EffectResolver, cache: Cache) {
  private val maybeLeague = settings.maybeLeaguePointScoring.map(_.league)

  def findAsync(
    forest: Forest[Step.ID, Step],
    signal: dom.AbortSignal
  ): Future[Option[Map[Step.ID, List[String]]]] =
    ForestFolder.foldLeftAsync(
      forest,
      (Map.empty[Step.ID, List[String]], Map.empty[Effect, Step.ID], Map.empty[Step.ID, Int], settings.initialPlayer),
      signal,
      _.repetitions
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
              if (reps > 1 || overallReps > 1) stepErrors.map(e => s"Rep $overallReps: $e")
              else stepErrors
            val newErrors = if (prefixedErrors.isEmpty) errors else errors + (step.id -> prefixedErrors)
            (newErrors, updatedOwners, updatedPlayer)
          }
        }
      (finalErrors, finalOwners, newProcessedReps, finalPlayer)
    }
    .map(_.map { case (errors, _, _, _) => errors })

  private def advancePlayer(player: Player, step: Step): Player =
    step.directEffects.underlying.foldLeft(player)(resolver.resolve)

  private def validateStep(
    stepId: Step.ID,
    player: Player,
    step: Step,
    completionOwners: Map[Effect, Step.ID]
  ): (List[String], Player, Map[Effect, Step.ID]) = {
    val requirementErrors = RequirementValidator.validate(step.requirements)(player, maybeLeague, cache)
    val (effectErrors, postEffectsPlayer, updatedOwners) =
      step.directEffects.underlying.foldLeft((List.empty[String], player, completionOwners)) {
        case ((errorAcc, preEffectPlayer, owners), effect) =>
          val postEffectPlayer = resolver.resolve(preEffectPlayer, effect)
          val errors =
            if (owners.get(effect).contains(stepId)) List.empty
            else EffectValidator.validate(effect)(preEffectPlayer, postEffectPlayer, maybeLeague, cache)
          val newOwners = recordOwnership(effect, stepId, owners)
          (errorAcc ++ errors, postEffectPlayer, newOwners)
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
