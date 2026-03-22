package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.projection.model.Projection
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

final class Projector(settings: Plan.Settings, effectResolver: EffectResolver) {
  /** Computes the [[Projection]] for the focused step asynchronously, yielding to the
    * macrotask queue periodically. This keeps the worker responsive — if a newer message
    * arrives mid-computation, the signal is aborted and [[None]] is returned early.
    *
    * The computation has three phases:
    *   1. Fold all steps preceding the focused step's root tree → playerAfterPreceding
    *   2. Fold the focused step's root tree up to (not including) the focus → playerBeforeFocus
    *      Ancestor steps are capped at one repetition so the "before" view shows the first
    *      iteration of any enclosing loop, keeping the UI free of confusing accumulation.
    *   3. Fold all ancestor loop iterations fully, then the focus subtree → playerAfterAllReps
    */
  def computeAsync(
    forest: Forest[Step.ID, Step],
    focusID: Option[Step.ID],
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

    val focusedStep = focusID.flatMap(forest.get)

    // Ancestors from outermost (root) to innermost (direct parent of focus)
    val ancestorsOuterFirst = focusID match {
      case Some(id) => containingTree.ancestors(id).reverse
      case None => List.empty
    }

    // Each ancestor paired with the next step on the path toward focus
    val ancestorPairs = ancestorsOuterFirst.zip(ancestorsOuterFirst.drop(1) ++ focusID.toList)

    // Subtree rooted at focus (focus + all descendants, full reps)
    val focusSubtree = focusID match {
      case Some(id) => containingTree.subtree(id)
      case None => Forest.empty
    }

    // Phase 1
    foldLeftAsync(precedingRootSteps, settings.initialPlayer, signal)(resolvePlayer).flatMap {
      case None => Future.successful(None)
      case Some(playerAfterPreceding) =>
        // Phase 2: playerBeforeFocus
        val transformedContainingTree = focusID match {
          case None => Forest.empty
          case Some(focus) =>
            val ancestors = containingTree.ancestors(focus)
            containingTree.takeUntil(focus).map((id, step) =>
              if (ancestors.contains(id)) step.deepCopy(repetitions = Math.min(step.repetitions, 1))
              else step
            )
        }

        foldLeftAsync(transformedContainingTree, playerAfterPreceding, signal)(resolvePlayer).flatMap {
          case None => Future.successful(None)
          case Some(playerBeforeFocus) =>
            val playerAfterEffects = focusedStep match {
              case Some(step) => effectResolver.resolve(playerBeforeFocus, step.directEffects.underlying*)
              case None => playerBeforeFocus
            }

            // Phase 3: playerAfterAllReps — replay all ancestor loop iterations fully,
            // then run the focus subtree one final time.
            // For each (ancestorId, nextId) pair (outermost first):
            //   Phase 3.1: fold the full ancestor subtree (reps-1) times
            //   Phase 3.2: fold the prefix of the ancestor subtree up to (not including) nextId once
            // Then fold the focus subtree.
            val playerAfterAllAncestorReps = ancestorPairs.foldLeft(
              Future.successful(Option(playerAfterPreceding))
            ) { case (futurePlayer, (ancestorId, nextId)) =>
              futurePlayer.flatMap {
                case None => Future.successful(None)
                case Some(player) =>
                  val ancestorSubtree = containingTree.subtree(ancestorId)
                  val reducedSubtree = ancestorSubtree.map((id, step) =>
                    if (id == ancestorId)
                      step.deepCopy(repetitions = Math.max(0, step.repetitions - 1))
                    else step
                  )
                  foldLeftAsync(reducedSubtree, player, signal)(resolvePlayer).flatMap {
                    case None => Future.successful(None)
                    case Some(playerAfterAncestorReps) =>
                      val prefix = ancestorSubtree.takeUntil(nextId).map((id, step) =>
                        if (id == ancestorId) step.deepCopy(repetitions = 1) else step
                      )
                      foldLeftAsync(prefix, playerAfterAncestorReps, signal)(resolvePlayer)
                  }
              }
            }

            playerAfterAllAncestorReps.flatMap {
              case None => Future.successful(None)
              case Some(playerBeforeLastRep) =>
                foldLeftAsync(focusSubtree, playerBeforeLastRep, signal)(resolvePlayer)
                  .map(_.map(playerAfterAllReps =>
                    Projection(playerBeforeFocus, playerAfterEffects, playerAfterAllReps)
                  ))
            }
        }
    }
  }

  private def resolvePlayer(player: Player, step: Step, reps: Int): Player =
    effectResolver.resolve(player, List.fill(reps)(step.directEffects.underlying).flatten*)

  private def foldLeftAsync[Acc](
    forest: Forest[Step.ID, Step],
    acc: Acc,
    signal: dom.AbortSignal
  )(f: (Acc, Step, Int) => Acc): Future[Option[Acc]] =
    ForestFolder.foldLeftAsync(forest, acc, signal, _.repetitions)(f)
}
