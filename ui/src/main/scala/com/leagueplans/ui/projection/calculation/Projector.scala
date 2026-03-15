package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.model.Projection
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

object Projector {
  def apply(settings: Plan.Settings, cache: Cache): Projector =
    new Projector(EffectResolver(settings, cache))

  private[calculation] type Iteration[ID, T] = (id: ID, value: T, reps: Int)

  private[calculation] def foldLeftAsyncHelper[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    initialAcc: Acc,
    initialRemaining: List[Iteration[ID, T]],
    signal: dom.AbortSignal,
    yieldInterval: Int
  )(f: (Acc, T, Int) => Acc): Future[Option[Acc]] = {
    var acc = initialAcc
    var remaining = initialRemaining
    var repsUntilYield = yieldInterval

    while (remaining.nonEmpty && repsUntilYield > 0 && !signal.aborted) {
      remaining match {
        case Nil => // unreachable; while condition ensures nonEmpty
        case h :: t =>
          if (h.reps <= 0) {
            remaining = t
          } else {
            val children = forest.children(h.id)
            if (children.isEmpty) {
              val excessReps = h.reps - repsUntilYield
              if (excessReps > 0) {
                acc = f(acc, h.value, repsUntilYield)
                remaining = (h.id, h.value, excessReps) +: t
                repsUntilYield = 0
              } else {
                acc = f(acc, h.value, h.reps)
                remaining = t
                repsUntilYield = -excessReps
              }
            } else {
              acc = f(acc, h.value, 1)
              remaining = (children :+ (h.id, h.value, h.reps - 1)) ++ t
              repsUntilYield -= 1
            }
          }
      }
    }

    if (signal.aborted) Future.successful(None)
    else if (remaining.isEmpty) Future.successful(Some(acc))
    else
      Future.unit.flatMap(_ =>
        foldLeftAsyncHelper(forest, acc, remaining, signal, yieldInterval)(f)
      )
  }
}

final class Projector(effectResolver: EffectResolver) {
  /** Computes the [[Projection]] for the focused step asynchronously, yielding to the
    * macrotask queue periodically. This keeps the worker responsive — if a newer message
    * arrives mid-computation, the signal is aborted and [[None]] is returned early.
    *
    * The computation has two phases:
    *   1. Fold all steps preceding the focused step's root tree → player state
    *   2. Fold the focused step's root tree up to (not including) the focus → refine player state
    *
    * In phase 2, ancestor steps are capped at one repetition. When a user focuses a step
    * inside a repeated ancestor, they're typically building out a single iteration of that
    * loop. Showing player state relative to the first iteration keeps the UI accurate and
    * avoids confusing errors — e.g. an inventory appearing to hold more items than any
    * single pass through the loop would produce. The confusion compounds quickly with
    * longer or nested loops.
    */
  def computeAsync(
    forest: Forest[Step.ID, Step],
    focusID: Option[Step.ID],
    settings: Plan.Settings,
    signal: dom.AbortSignal
  ): Future[Option[Projection]] = {
    val containingTree = focusID match {
      case Some(id) =>
        val root = forest.ancestors(id).lastOption.getOrElse(id)
        forest.subtree(root)
      case None =>
        Forest.empty
    }

    val precedingRootSteps = containingTree.roots.headOption match {
      case Some(root) => forest.takeUntil(root)
      case None => forest
    }

    // Phase 1
    foldLeftAsync(precedingRootSteps, settings.initialPlayer, signal)(resolvePlayer).flatMap {
      case None => Future.successful(None)
      case Some(playerAfterPreceding) =>
        // Phase 2
        val transformedContainingTree = focusID match {
          case None => Forest.empty
          case Some(focus) =>
            val ancestors = containingTree.ancestors(focus)
            containingTree.takeUntil(focus).map((id, step) =>
              if (ancestors.contains(id)) step.deepCopy(repetitions = Math.min(step.repetitions, 1))
              else step
            )
        }

        foldLeftAsync(
          transformedContainingTree,
          playerAfterPreceding,
          signal
        )(resolvePlayer).map(_.map { playerBeforeFocus =>
          val playerAfterFirstCompletion = focusID.flatMap(forest.get) match {
            case Some(step) => effectResolver.resolve(playerBeforeFocus, step.directEffects.underlying*)
            case None => playerBeforeFocus
          }
          Projection(playerAfterFirstCompletion)
        })
    }
  }

  private def resolvePlayer(player: Player, step: Step, reps: Int): Player =
    effectResolver.resolve(player, List.fill(reps)(step.directEffects.underlying).flatten*)

  private def foldLeftAsync[Acc](
    forest: Forest[Step.ID, Step],
    acc: Acc,
    signal: dom.AbortSignal,
    yieldInterval: Int = 500
  )(f: (Acc, Step, Int) => Acc): Future[Option[Acc]] = {
    val iterationForest = forest.map[Projector.Iteration[Step.ID, Step]]((id, step) => (id, step, step.repetitions))
    val roots = iterationForest.roots.flatMap(iterationForest.get)
    Projector.foldLeftAsyncHelper(iterationForest, acc, roots, signal, yieldInterval)(f)
  }
}
