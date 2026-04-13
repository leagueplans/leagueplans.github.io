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
  leagues1Props: Option[LeagueTaskTier] = None,
  leagues2Props: Option[TrailblazerTaskProperties] = None,
  leagues3Props: Option[ShatteredRelicsTaskProperties] = None,
  leagues4Props: Option[TrailblazerTaskProperties] = None,
  leagues5Props: Option[TrailblazerTaskProperties] = None,
  leagues6Props: Option[TrailblazerTaskProperties] = None
)
