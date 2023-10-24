package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait LeagueTaskTier

object LeagueTaskTier {
  case object Beginner extends LeagueTaskTier
  case object Easy extends LeagueTaskTier
  case object Medium extends LeagueTaskTier
  case object Hard extends LeagueTaskTier
  case object Elite extends LeagueTaskTier
  case object Master extends LeagueTaskTier

  val all: Set[LeagueTaskTier] =
    Set(Beginner, Easy, Medium, Hard, Elite, Master)

  implicit val codec: Codec[LeagueTaskTier] = deriveCodec[LeagueTaskTier]
}