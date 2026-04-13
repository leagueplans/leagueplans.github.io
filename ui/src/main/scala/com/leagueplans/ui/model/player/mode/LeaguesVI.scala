package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.Skill.{Herblore, Hitpoints, Runecraft}
import com.leagueplans.common.model.{Item, LeagueTaskTier, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplier, LeaguePointScoring, Plan}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object LeaguesVI extends Mode.League {
  val name: String = "Leagues VI: Demonic Pacts"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        stats = Stats(
          Herblore -> Level(3).bound,
          Hitpoints -> Level(10).bound,
          Runecraft -> Level(5).bound
        ),
        depositories = MainGame.initialPlayer.depositories + (
          Depository.Kind.Inventory -> Depository(
            MainGame.initialInventory + ((Item.ID(3737), false) -> 1), // Dramen staff
            Depository.Kind.Inventory
          )
        ),
        completedQuests = Set(
          3, // Restless Ghost
          17, // Dragon Slayer
          18, // Druidic Ritual
          19, // Lost City
          53, // Rune Mysteries
          55, // Elemental Workshop
          56, // Priest in Peril
          57, // Nature Spirit
          105, // Fairytale I
          122, // Fairytale II
          130, // Eagles' Peak
          184, // Desert Treasure II - The Fallen Empire
          186, // Children of the Sun
          188, // Twilight's Promise
          190, // Perilous Moons
          202 // Learning the Ropes
        ),
        leagueStatus = LeagueStatus(
          leaguePoints = 0,
          completedTasks = Set.empty,
          skillsUnlocked = Skill.values.toSet
        )
      ),
      expMultipliers = List(
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Multiplicative,
          base = 5.0,
          thresholds = List(
            8.0 -> ExpMultiplier.Condition.LeaguePoints(750),
            12.0 -> ExpMultiplier.Condition.LeaguePoints(5000),
            16.0 -> ExpMultiplier.Condition.LeaguePoints(16000),
          )
        ),
        ExpMultiplier(
          Skill.combats,
          ExpMultiplier.Kind.Multiplicative,
          base = 1.0,
          thresholds = List(
            1.5 -> ExpMultiplier.Condition.LeaguePoints(1500)
          )
        )
      ),
      maybeLeaguePointScoring = Some(LeaguePointScoring(
        LeaguesVI,
        Map(
          LeagueTaskTier.Easy -> 10,
          LeagueTaskTier.Medium -> 30,
          LeagueTaskTier.Hard -> 80,
          LeagueTaskTier.Elite -> 200,
          LeagueTaskTier.Master -> 400
        )
      ))
    )
}
