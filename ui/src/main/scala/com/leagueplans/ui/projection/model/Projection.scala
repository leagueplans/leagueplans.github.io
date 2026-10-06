package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.Player

object Projection {
  given Encoder[Projection] = Encoder.derived
  given Decoder[Projection] = Decoder.derived

  def apply(settings: Plan.Settings): Projection =
    Projection(
      playerBeforeStep = settings.initialPlayer,
      playerAfterEffects = settings.initialPlayer,
      playerAfterAllReps = settings.initialPlayer,
      focusID = None
    )
}

/** The players around the focused step
  *
  * @param focusID the step these were worked out for. Until the worker catches up with a change
  *                of focus, the players on show belong to the step that was focused before.
  */
final case class Projection(
  playerBeforeStep: Player,
  playerAfterEffects: Player,
  playerAfterAllReps: Player,
  focusID: Option[Step.ID]
)
