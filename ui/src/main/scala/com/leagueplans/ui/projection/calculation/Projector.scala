package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Repetitions, Step}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.projection.model.Projection
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/** @param initialPlayer the player before the plan's first step */
final class Projector(initialPlayer: Player, effectResolver: EffectResolver, cache: Cache) {
  private val looping = StepLooping[Player](effectResolver, cache, identity)

  /** Computes the [[Projection]] for the focused step asynchronously, yielding to the
    * macrotask queue periodically. This keeps the worker responsive — if a newer message
    * arrives mid-computation, the signal is aborted and [[None]] is returned early.
    *
    * The computation has three phases:
    *   1. Fold all steps preceding the focused step's root tree → playerAfterPreceding
    *   2. Fold the focused step's root tree up to (not including) the focus → playerBeforeFocus
    *      Ancestor steps are capped at one repetition so the "before" view shows the first
    *      iteration of any enclosing loop, keeping the UI free of confusing accumulation. An
    *      ancestor that repeats until a condition is met plays once here even if the condition was
    *      already met, so that the view still shows where the step would be.
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
    foldLeftAsync(precedingRootSteps, (initialPlayer, None), signal).flatMap {
      case None => Future.successful(None)
      case Some(afterPreceding) =>
        // Phase 2: playerBeforeFocus
        val transformedContainingTree = focusID match {
          case None => Forest.empty
          case Some(focus) =>
            val ancestors = containingTree.ancestors(focus)
            containingTree.takeUntil(focus).map((id, step) =>
              if (ancestors.contains(id)) step.deepCopy(repetitions = firstRepOnly(step.repetitions))
              else step
            )
        }

        foldLeftAsync(transformedContainingTree, afterPreceding, signal).flatMap {
          case None => Future.successful(None)
          case Some((playerBeforeFocus, _)) =>
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
              Future.successful(Option(afterPreceding))
            ) { case (futurePlayer, (ancestorId, nextId)) =>
              futurePlayer.flatMap {
                case None => Future.successful(None)
                case Some(at) =>
                  val ancestorSubtree = containingTree.subtree(ancestorId)
                  // A repeat until a condition is met plays out in full, so that the condition is
                  // checked as in the rest of the plan, and its last repetition is withheld
                  val reducedSubtree = ancestorSubtree.map((id, step) =>
                    (id, step.repetitions) match {
                      case (`ancestorId`, Repetitions.Fixed(n)) => step.deepCopy(repetitions = Repetitions.Fixed(Math.max(0, n - 1)))
                      case _ => step
                    }
                  )
                  foldLeftAsync(reducedSubtree, at, signal, withholdLastRepOf = Some(ancestorId)).flatMap {
                    case None => Future.successful(None)
                    case Some(afterAncestorReps) =>
                      val prefix = ancestorSubtree.takeUntil(nextId).map((id, step) =>
                        if (id == ancestorId) step.deepCopy(repetitions = Repetitions.once) else step
                      )
                      foldLeftAsync(prefix, afterAncestorReps, signal)
                  }
              }
            }

            playerAfterAllAncestorReps.flatMap {
              case None => Future.successful(None)
              case Some(beforeLastRep) =>
                foldLeftAsync(focusSubtree, beforeLastRep, signal)
                  .map(_.map((playerAfterAllReps, _) =>
                    Projection(playerBeforeFocus, playerAfterEffects, playerAfterAllReps, focusID)
                  ))
            }
        }
    }
  }

  private def resolvePlayer(player: Player, step: Step, reps: Int): Player =
    effectResolver.resolve(player, List.fill(reps)(step.directEffects.underlying).flatten*)

  private def firstRepOnly(repetitions: Repetitions): Repetitions =
    repetitions match {
      case Repetitions.Fixed(n) => Repetitions.Fixed(Math.min(n, 1))
      case Repetitions.Until(_, _) => Repetitions.once
    }

  /** Folds from a player, and how far into the plan they are, to the same after the steps */
  private def foldLeftAsync(
    forest: Forest[Step.ID, Step],
    from: (Player, Option[Duration]),
    signal: dom.AbortSignal,
    withholdLastRepOf: Option[Step.ID] = None
  ): Future[Option[(Player, Option[Duration])]] =
    ForestFolder.foldLeftAsync(forest, from._1, signal, looping, from._2, withholdLastRepOf)(resolvePlayer)
      .map(_.map(result => (result.acc, result.elapsed)))
}
