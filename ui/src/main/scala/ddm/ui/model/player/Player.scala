package ddm.ui.model.player

import ddm.ui.model.player.item.{Depository, Equipment}
import ddm.ui.model.player.skill.Stats
import ddm.ui.model.player.league.{LeagueStatus, Task}

object Player {
  def initial: Player =
    Player(
      Stats.initial,
      Map(
        "Inventory" -> Depository.inventory,
        "Bank" -> Depository.bank
      ),
      Equipment.initial,
      questPoints = 0,
      leagueStatus = LeagueStatus.initial
    )
}

final case class Player(
  stats: Stats,
  depositories: Map[String, Depository],
  equipment: Equipment,
  questPoints: Int,
  leagueStatus: LeagueStatus
)
