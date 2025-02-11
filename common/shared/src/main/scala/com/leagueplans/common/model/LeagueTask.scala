package com.leagueplans.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object LeagueTask {
  given Codec[LeagueTask] = deriveCodec
  given Ordering[LeagueTask] = Ordering.by(_.id)
}

final case class LeagueTask(
  id: Int,
  name: String,
  description: String,
  leagues1Props: Option[LeagueTaskTier],
  leagues2Props: Option[TrailblazerTaskProperties],
  leagues3Props: Option[ShatteredRelicsTaskProperties],
  leagues4Props: Option[TrailblazerTaskProperties],
  leagues5Props: Option[TrailblazerTaskProperties]
)
