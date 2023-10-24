package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object LeagueTask {
  implicit val codec: Codec[LeagueTask] = deriveCodec
  implicit val ordering: Ordering[LeagueTask] = Ordering.by(_.id)
}

final case class LeagueTask(
  id: Int,
  name: String,
  description: String,
  leagues1Props: Option[LeagueTaskTier],
  leagues2Props: Option[TrailblazerTaskProperties],
  leagues3Props: Option[ShatteredRelicsTaskProperties]
)
