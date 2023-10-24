package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object TrailblazerTaskProperties {
  implicit val codec: Codec[TrailblazerTaskProperties] = deriveCodec
}

final case class TrailblazerTaskProperties(tier: LeagueTaskTier, area: LeagueTaskArea)
