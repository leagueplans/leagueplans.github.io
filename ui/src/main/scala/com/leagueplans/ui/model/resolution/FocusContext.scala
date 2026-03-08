package com.leagueplans.ui.model.resolution

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.plan.Step.ID
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.resolution.{EffectResolver, StepSeries}
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.StrictSignal

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
    focus.map(_.exists(_.id == id))

  private val containingTree: Signal[Option[Forest[Step.ID, Step]]] =
    Signal.combine(focusID, forest).map((maybeID, f) =>
      maybeID.map { id =>
        val root = f.ancestors(id).lastOption.getOrElse(id)
        f.subtree(root)
      }
    )

  private val precedingRootSteps: Signal[Forest[Step.ID, Step]] =
    Signal.combine(containingTree, forest).map((maybeTree, f) =>
      maybeTree.flatMap(_.roots.headOption) match {
        case Some(root) => f.takeUntil(root)
        case None => f
      }
    ).distinct

  private val playerAfterPrecedingRootSteps: Signal[Player] =
    precedingRootSteps.map(resolveEffects(_, initialPlayer))

  private val playerBeforeCurrentFocus: Signal[Player] =
    Signal.combine(focusID, containingTree, playerAfterPrecedingRootSteps).map {
      case (Some(focus), Some(tree), player) =>
        val ancestors = tree.ancestors(focus)
        val priorSteps = tree.takeUntil(focus).map((id, step) =>
          if (ancestors.contains(id))
            // We override ancestors to use at most one repetition because it's more
            // helpful to the user if they can see what the state is on the first
            // iteration of the focused step
            step.deepCopy(repetitions = Math.min(step.repetitions, 1))
          else
            step
        )
        resolveEffects(priorSteps, player)

      case (_, _, player) =>
        player
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

  private def resolveEffects(forest: Forest[ID, Step], player: Player) =
    StepSeries.foldLeft(forest, player)((player, step, repetitions) =>
      effectResolver.resolve(player, List.fill(repetitions)(step.directEffects.underlying).flatten *)
    )
}
