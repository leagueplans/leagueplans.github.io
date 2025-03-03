package com.leagueplans.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object TrailblazerTaskProperties {
  given Codec[TrailblazerTaskProperties] = deriveCodec
}

final case class TrailblazerTaskProperties(tier: LeagueTaskTier, area: LeagueTaskArea)
