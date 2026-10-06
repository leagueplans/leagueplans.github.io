package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.{Player, ViewerAccount}

object Projection {
  given Encoder[Projection] = Encoder.derived
  given Decoder[Projection] = Decoder.derived

  /** Before anything's focused, from the plan's starting player */
  def apply(initialPlayer: Player): Projection =
    Projection(
      playerBeforeStep = initialPlayer,
      playerAfterEffects = initialPlayer,
      playerAfterAllReps = initialPlayer,
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
) {
  /** The players as they'd be with the viewer's account, which the page applies straight away
    * rather than waiting for the worker to work everything out again. Nothing in a plan depends on
    * the account but the bank's capacity. */
  def withAccount(account: ViewerAccount): Projection =
    copy(
      playerBeforeStep = account.applyTo(playerBeforeStep),
      playerAfterEffects = account.applyTo(playerAfterEffects),
      playerAfterAllReps = account.applyTo(playerAfterAllReps)
    )
}
