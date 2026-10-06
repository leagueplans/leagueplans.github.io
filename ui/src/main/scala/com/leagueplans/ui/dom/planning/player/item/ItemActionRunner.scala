package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.ItemActions.Action
import com.raquo.airstream.core.{Observer, Signal}

/** Adds the effects of the Items section's actions to the focused step, and reports each in a
  * toast with Undo.
  *
  * @param playerAtInsertion the state new effects are applied to, which actions are worked out from
  */
final class ItemActionRunner(
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  val playerAtInsertion: Signal[Player],
  undoToasts: UndoToasts
) {
  /** Runs an action, while a step is focused. Sample it where the action is triggered. */
  val run: Signal[Option[Action => Unit]] =
    effectObserver.map(_.map(observer => action => {
      observer.onNext(action.effects)
      undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
    }))

  /** Whether a step is focused, for actions to run on */
  val canRun: Signal[Boolean] =
    effectObserver.map(_.nonEmpty).distinct
}
