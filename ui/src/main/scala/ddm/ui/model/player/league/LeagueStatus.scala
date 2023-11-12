package ddm.ui.model.player.league

import ddm.common.model.Skill

final case class LeagueStatus(
  leaguePoints: Int,
  completedTasks: Set[Int],
  skillsUnlocked: Set[Skill],
  expMultiplierStrategy: ExpMultiplierStrategy
)
