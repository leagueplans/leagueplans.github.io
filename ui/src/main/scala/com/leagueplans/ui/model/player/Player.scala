package com.leagueplans.ui.model.player

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats

object Player {
  private final case class Simplified(
    stats: Stats,
    depositories: Iterable[Depository],
    completedQuests: Set[Int],
    completedDiaryTasks: Set[Int],
    leagueStatus: LeagueStatus
  )

  given Encoder[Player] = Encoder.derived[Simplified].contramap(player =>
    Simplified(
      player.stats,
      player.depositories.values,
      player.completedQuests,
      player.completedDiaryTasks,
      player.leagueStatus
    )
  )

  given Decoder[Player] = Decoder.derived[Simplified].map(simplified =>
    Player(
      simplified.stats,
      simplified.depositories.map(d => d.kind -> d).toMap,
      simplified.completedQuests,
      simplified.completedDiaryTasks,
      simplified.leagueStatus
    )
  )
}

final case class Player(
  stats: Stats,
  depositories: Map[Depository.Kind, Depository],
  completedQuests: Set[Int],
  completedDiaryTasks: Set[Int],
  leagueStatus: LeagueStatus
) {
  def get(kind: Depository.Kind): Depository =
    depositories.getOrElse(kind, Depository.empty(kind))
}
