package ddm.ui.model.player

import ddm.ui.model.player.item.{Depository, Equipment}
import ddm.ui.model.player.league.LeagueStatus
import ddm.ui.model.player.skill.Stats

object Player {
  def initial: Player =
    Player(
      Stats.initial,
      Equipment.initial.raw ++ List(
        Depository.inventory,
        Depository.bank
      ).map(d => d.id -> d).toMap,
      completedQuests = Set.empty,
      leagueStatus = LeagueStatus.initial
    )
}

final case class Player(
  stats: Stats,
  depositories: Map[Depository.ID, Depository],
  completedQuests: Set[Quest],
  leagueStatus: LeagueStatus
) {
  val questPoints: Int =
    completedQuests.toList.map(_.points).sum
}
