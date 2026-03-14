package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.decoding.Decoder.durationDecoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.encoding.Encoder.durationEncoder
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.Player

import scala.concurrent.duration.Duration

object Projection {
  given Encoder[Projection] = Encoder.derived
  given Decoder[Projection] = Decoder.derived

  def apply(settings: Plan.Settings): Projection =
    Projection(
      playerAfterFirstCompletion = settings.initialPlayer,
      timeBeforeCurrentFocus = Duration.Zero,
      timeAfterCurrentFocus = Duration.Zero,
      durationOfRep = Duration.Zero,
      ancestorRepetitions = 1
    )
}

final case class Projection(
  playerAfterFirstCompletion: Player,
  timeBeforeCurrentFocus: Duration,
  timeAfterCurrentFocus: Duration,
  durationOfRep: Duration,
  ancestorRepetitions: Int
)
