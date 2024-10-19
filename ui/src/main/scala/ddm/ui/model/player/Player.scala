package ddm.ui.model.player

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.league.LeagueStatus
import ddm.ui.model.player.skill.Stats

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
