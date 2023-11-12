package ddm.ui.model.player.mode

import ddm.common.model.Skill
import ddm.ui.model.player.Player
import ddm.ui.model.player.league.{ExpMultiplierStrategy, LeagueStatus}

object LeaguesIII extends Mode.League {
  val name: String = "Leagues III: Shattered Relics"

  val initialPlayer: Player =
    MainGame.initialPlayer.copy(
      leagueStatus = LeagueStatus(
        leaguePoints = 0,
        completedTasks = Set.empty,
        skillsUnlocked = Set(
          Skill.Defence,
          Skill.Fishing,
          Skill.Thieving
        ),
        ExpMultiplierStrategy.LeaguePointBased(5, List(300 -> 8, 3000 -> 12, 15000 -> 16))
      ),
      mode = LeaguesIII
    )
}
