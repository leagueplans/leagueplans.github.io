package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.player.Player

object Plan {
  given Encoder[Plan] = Encoder.derived
  given Decoder[Plan] = Decoder.derived

  object Settings {
    given Encoder[Settings] = Encoder.derived
    given Decoder[Settings] = Decoder.derived
  }

  final case class Settings(
    initialPlayer: Player,
    expMultiplierStrategy: ExpMultiplierStrategy,
    maybeLeaguePointScoring: Option[LeaguePointScoring]
  )
}

final case class Plan(
  name: String,
  steps: Forest[Step.ID, Step],
  settings: Plan.Settings
)
