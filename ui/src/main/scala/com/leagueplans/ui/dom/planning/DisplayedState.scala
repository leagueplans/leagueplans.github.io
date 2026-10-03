package com.leagueplans.ui.dom.planning

import com.leagueplans.ui.model.player.{FocusContext, Player}
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var

/** The player states the planning page shows for the focused step */
final class DisplayedState(focusContext: FocusContext) {
  val renderMode: Var[RenderMode] = Var(RenderMode.AfterEffects)

  /** The state chosen by the render mode */
  val displayedPlayer: Signal[Player] =
    renderMode.signal.flatMapSwitch {
      case RenderMode.Before => focusContext.playerBeforeCurrentFocus
      case RenderMode.AfterEffects => focusContext.playerAfterEffectsOfCurrentFocus
      case RenderMode.AfterAllReps => focusContext.playerAfterAllRepsOfCurrentFocus
    }

  /** The state that new effects on the focused step are applied to, whatever the render mode */
  val playerAtInsertion: Signal[Player] =
    focusContext.playerAfterEffectsOfCurrentFocus

  /** The state before the focused step, for showing what the step changed */
  val baseline: Signal[Option[Player]] =
    Signal.combine(focusContext.focus, focusContext.playerBeforeCurrentFocus).map((maybeStep, player) =>
      maybeStep.map(_ => player)
    )
}
