package com.leagueplans.ui.model.player

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.plan.Step.ID
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.projection.model.Projection
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.StrictSignal

/** Thin wrapper over worker-computed Projections, with cheap synchronous signals kept local. */
final class FocusContext(
  val focusID: StrictSignal[Option[Step.ID]],
  forest: StrictSignal[Forest[Step.ID, Step]],
  projection: Signal[Projection]
) {
  val focus: Signal[Option[Step]] =
    Signal.combine(focusID, forest).map((maybeID, f) =>
      maybeID.flatMap(f.get)
    ).distinct

  def signalFor(id: Step.ID): Signal[Boolean] =
    focus.map(_.exists(_.id == id)).distinct

  val playerBeforeCurrentFocus: Signal[Player] =
    projection.map(_.playerBeforeStep)

  val playerAfterEffectsOfCurrentFocus: Signal[Player] =
    projection.map(_.playerAfterEffects)

  val playerAfterAllRepsOfCurrentFocus: Signal[Player] =
    projection.map(_.playerAfterAllReps)
}
