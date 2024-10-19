package ddm.ui.model.player.league

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.common.model.Skill

object LeagueStatus {
  given Encoder[LeagueStatus] = Encoder.derived
  given Decoder[LeagueStatus] = Decoder.derived
}

final case class LeagueStatus(
  leaguePoints: Int,
  completedTasks: Set[Int],
  skillsUnlocked: Set[Skill]
)
