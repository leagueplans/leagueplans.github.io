package com.leagueplans.ui.model.resolution

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.plan.Step.ID
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.resolution.{EffectResolver, StepSeries}
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.StrictSignal

import scala.concurrent.duration.{Duration, FiniteDuration}

/** Memoising wrapper around compute-intensive calculations */
final class FocusContext(
  initialPlayer: Player,
  val focusID: StrictSignal[Option[Step.ID]],
  forest: StrictSignal[Forest[Step.ID, Step]],
  effectResolver: EffectResolver
) {
  val focus: Signal[Option[Step]] =
    Signal.combine(focusID, forest).map((maybeID, f) =>
      maybeID.flatMap(f.get)
    ).distinct

  def signalFor(id: Step.ID): Signal[Boolean] =
    focus.map(_.exists(_.id == id)).distinct

  private val containingTree: Signal[Option[Forest[Step.ID, Step]]] =
    Signal.combine(focusID, forest).map((maybeID, f) =>
      maybeID.map { id =>
        val root = f.ancestors(id).lastOption.getOrElse(id)
        f.subtree(root)
      }
    ).distinct

  // Consider a step with an ancestor that repeats. When the user focuses
  // that step, they'll usually be in the process of building out the
  // repetition cycle of the ancestor. It's less confusing in these cases
  // if we show the user the player state relative to the first iteration
  // of ancestor steps.
  //
  // For example, let's say the user is setting up a loop where they'll
  // cycle between chopping 20 logs and banking them for 10 times. The
  // user has set up the log chopping step, and is now setting up the
  // banking step. If we were to visualise the banking step for anything
  // other than the first iteration, then we'd show at least 40 logs in
  // the player's inventory (two iterations worth).
  //
  // When the user goes to bank the logs, our UI components won't
  // accurately limit the user to only banking 20 logs at a time. If the
  // user accidentally banks more than that, then they'll start seeing
  // errors in their route, as they're banking more logs than they've
  // gathered. We've pushed some additional overhead onto the user, as
  // they need to go back through the previous steps in the loop to try
  // to figure out what the correct number to bank is.
  //
  // This isn't too bad in our simple example, but the complexity
  // increases exponentially for lengthier and/or nested loops. As such,
  // we override ancestors to use at most one repetition.
  private val transformedContainingTree =
    Signal.combine(focusID, containingTree).map((maybeFocus, maybeTree) =>
      maybeFocus.zip(maybeTree).map { (focus, tree) =>
        val ancestors = tree.ancestors(focus)
        tree.takeUntil(focus).map((id, step) =>
          if (ancestors.contains(id))
            step.deepCopy(repetitions = Math.min(step.repetitions, 1))
          else
            step
        )
      }
    ).distinct

  private val precedingRootSteps: Signal[Forest[Step.ID, Step]] =
    Signal.combine(containingTree, forest).map((maybeTree, f) =>
      maybeTree.flatMap(_.roots.headOption) match {
        case Some(root) => f.takeUntil(root)
        case None => f
      }
    ).distinct

  private val playerAfterPrecedingRootSteps: Signal[Player] =
    precedingRootSteps.map(resolveEffects(_, initialPlayer)).distinct

  private val timeAfterPrecedingRootSteps: Signal[FiniteDuration] =
    precedingRootSteps.map(resolveDuration(_, Duration.Zero)).distinct

  private val playerBeforeCurrentFocus: Signal[Player] =
    Signal.combine(transformedContainingTree, playerAfterPrecedingRootSteps).map {
      case (Some(tree), player) => resolveEffects(tree, player)
      case (None, player) => player
    }.distinct

  // TODO For time tracking, a separate time-keeping component may be better,
  //      keeping an index from step ID to start/end times. That would make it
  //      much more performant to show timestamps in step headers.
  val timeBeforeCurrentFocus: Signal[FiniteDuration] =
    Signal.combine(transformedContainingTree, timeAfterPrecedingRootSteps).map {
      case (Some(tree), duration) => resolveDuration(tree, duration)
      case (None, duration) => duration
    }.distinct

  val playerAfterFirstRepOfCurrentFocus: Signal[Player] =
    Signal.combine(focus, playerBeforeCurrentFocus).map {
      case (Some(step), player) => effectResolver.resolve(player, step.directEffects.underlying*)
      case (None, player) => player
    }.distinct

  val playerAfterAllRepsOfCurrentFocus: Signal[Player] =
    Signal.combine(focusID, containingTree, playerBeforeCurrentFocus).map {
      case (Some(step), Some(tree), player) => resolveEffects(tree.subtree(step), player)
      case (_, _, player) => player
    }.distinct

  val timeAfterAllRepsOfCurrentFocus: Signal[FiniteDuration] =
    Signal.combine(focusID, containingTree, timeBeforeCurrentFocus).map {
      case (Some(step), Some(tree), duration) => resolveDuration(tree.subtree(step), duration)
      case (_, _, duration) => duration
    }.distinct

  private def resolveEffects(forest: Forest[ID, Step], player: Player): Player =
    StepSeries.foldLeft(forest, player)((player, step, repetitions) =>
      effectResolver.resolve(player, List.fill(repetitions)(step.directEffects.underlying).flatten *)
    )

  private def resolveDuration(forest: Forest[ID, Step], duration: FiniteDuration): FiniteDuration =
    StepSeries.foldLeft(forest, duration)((duration, step, repetitions) =>
      duration + (step.duration.asScala * repetitions)
    )
}
