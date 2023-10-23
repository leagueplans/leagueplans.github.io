package ddm.ui.model.player.mode

import ddm.ui.model.player.Player
import ddm.ui.model.player.league.LeagueStatus
import ddm.ui.model.player.skill.Skill

object LeaguesIII extends Mode.League {
  val name: String = "Leagues III: Shattered Relics"

  val initialPlayer: Player = {
    MainGame.initialPlayer.copy(
      leagueStatus = LeagueStatus(
        multiplier = 5,
        tasksCompleted = Set.empty,
        skillsUnlocked = Set(
          Skill.Defence,
          Skill.Fishing,
          Skill.Thieving
        )
      )
    )
  }
}
