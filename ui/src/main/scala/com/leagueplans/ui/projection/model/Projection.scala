package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.Player

object Projection {
  given Encoder[Projection] = Encoder.derived
  given Decoder[Projection] = Decoder.derived

  def apply(settings: Plan.Settings): Projection =
    Projection(
      playerBeforeStep = settings.initialPlayer,
      playerAfterEffects = settings.initialPlayer,
      playerAfterAllReps = settings.initialPlayer
    )
}

final case class Projection(
  playerBeforeStep: Player,
  playerAfterEffects: Player,
  playerAfterAllReps: Player
)
