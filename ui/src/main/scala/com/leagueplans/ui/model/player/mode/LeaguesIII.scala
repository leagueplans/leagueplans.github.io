package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.{LeagueTaskTier, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplierStrategy, LeaguePointScoring, Plan}
import com.leagueplans.ui.model.player.league.LeagueStatus

object LeaguesIII extends Mode.League {
  val name: String = "Leagues III: Shattered Relics"

  val settings: Plan.Settings =
    Plan.Settings(
      initialPlayer = MainGame.initialPlayer.copy(
        leagueStatus = LeagueStatus(
          leaguePoints = 0,
          completedTasks = Set.empty,
          skillsUnlocked = Set(
            Skill.Defence,
            Skill.Fishing,
            Skill.Thieving
          )
        )
      ),
      expMultiplierStrategy = ExpMultiplierStrategy.LeaguePointBased(
        5,
        List(300 -> 8, 3000 -> 12, 15000 -> 16)
      ),
      maybeLeaguePointScoring = Some(LeaguePointScoring(
        LeaguesIII,
        Map(
          LeagueTaskTier.Beginner -> 5,
          LeagueTaskTier.Easy -> 5,
          LeagueTaskTier.Medium -> 25,
          LeagueTaskTier.Hard -> 50,
          LeagueTaskTier.Elite -> 125,
          LeagueTaskTier.Master -> 250
        )
      ))
    )
}
