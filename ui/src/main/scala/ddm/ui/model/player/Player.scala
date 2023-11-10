package ddm.ui.model.player

import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.league.LeagueStatus
import ddm.ui.model.player.mode.Mode
import ddm.ui.model.player.skill.Stats

final case class Player(
  stats: Stats,
  depositories: Map[Depository.Kind, Depository],
  completedQuests: Set[Int],
  completedDiaryTasks: Set[Int],
  leagueStatus: LeagueStatus,
  mode: Mode
) {
  def get(kind: Depository.Kind): Depository =
    depositories.getOrElse(kind, Depository.empty(kind))
}
