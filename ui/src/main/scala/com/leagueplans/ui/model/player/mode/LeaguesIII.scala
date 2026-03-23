package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.{LeagueTaskTier, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplier, LeaguePointScoring, Plan}
import com.leagueplans.ui.model.player.league.LeagueStatus

object LeaguesIII extends Mode.League {
  val name: String = "Leagues III: Shattered Relics"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        completedQuests = Set(
          202 // Learning the Ropes
        ),
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
      expMultipliers = List(ExpMultiplier(
        Skill.values.toSet,
        ExpMultiplier.Kind.Multiplicative,
        base = 5.0,
        thresholds = List(
          8.0 -> ExpMultiplier.Condition.LeaguePoints(300),
          12.0 -> ExpMultiplier.Condition.LeaguePoints(3000),
          16.0 -> ExpMultiplier.Condition.LeaguePoints(15000),
        )
      )),
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
