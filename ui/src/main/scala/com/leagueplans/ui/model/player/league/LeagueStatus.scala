package com.leagueplans.ui.model.player.league

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill

object LeagueStatus {
  given Encoder[LeagueStatus] = Encoder.derived
  given Decoder[LeagueStatus] = Decoder.derived
}

final case class LeagueStatus(
  leaguePoints: Int,
  completedTasks: Set[Int],
  skillsUnlocked: Set[Skill]
)
