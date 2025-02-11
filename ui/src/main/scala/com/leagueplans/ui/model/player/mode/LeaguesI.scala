package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.LeagueTaskTier
import com.leagueplans.common.model.Skill.{Agility, Herblore, Hitpoints}
import com.leagueplans.ui.model.plan.{ExpMultiplierStrategy, LeaguePointScoring, Plan}
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object LeaguesI extends Mode.League {
  val name: String = "Twisted League"

  val settings: Plan.Settings =
    Plan.Settings(
      initialPlayer = MainGame.initialPlayer.copy(
        stats = Stats(
          Agility -> Level(15).bound,
          Herblore -> Level(3).bound,
          Hitpoints -> Level(10).bound
        ),
        completedQuests = Set(
          17, // Dragon slayer
          18, // Druidic ritual
          53, // Rune mysteries
          130 // Eagle's peak
        ),
        completedDiaryTasks = Set(
          256, // Travel to the fairy ring south of Mount Karuulm
          278 // Cast Monster Examine on a mountain troll south of Mount Quidamortem
        )
      ),
      expMultiplierStrategy = ExpMultiplierStrategy.Fixed(5),
      maybeLeaguePointScoring = Some(LeaguePointScoring(
        LeaguesI,
        Map(
          LeagueTaskTier.Easy -> 10,
          LeagueTaskTier.Medium -> 50,
          LeagueTaskTier.Hard -> 100,
          LeagueTaskTier.Elite -> 250,
          LeagueTaskTier.Master -> 500
        )
      ))
    )
}
