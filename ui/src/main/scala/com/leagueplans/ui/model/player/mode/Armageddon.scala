package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.common.model.Skill.{Herblore, Hitpoints}
import com.leagueplans.ui.model.plan.{ExpMultiplier, Plan}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object Armageddon extends Mode.Deadman {
  val name: String = "Deadman: Armageddon"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        stats = Stats(
          Herblore -> Level(3).bound,
          Hitpoints -> Level(10).bound
        ),
        depositories = MainGame.initialPlayer.depositories + (
          Depository.Kind.Inventory -> Depository(
            MainGame.initialInventory + ((Item.ID(3262), false) -> 1), // Deadman starter pack
            Depository.Kind.Inventory
          )
        ),
        completedQuests = Set(
          3, // The Restless Ghost
          6, // Shield of Arrav
          7, // Ernest the Chicken
          15, // Goblin Diplomacy
          18, // Druidic Ritual
          19, // Lost City
          21, // Merlin's Crystal
          23, // Alfred Grimhand's Barcrawl
          31, // Holy Grail
          53, // Rune Mysteries
          56, // Priest in Peril
          57, // Nature Spirit
          105, // Fairytale I - Growing Pains
          122, // Fairytale II - Cure a Queen
          131, // Animal Magnetism
          202 // Learning the Ropes
        ),
        completedDiaryTasks = Set(
          193 // Read the blackboard at Barbarian Assault after reaching level 5 in every role
        )
      ),
      expMultipliers = List(
        ExpMultiplier(
          Skill.combats,
          ExpMultiplier.Kind.Multiplicative,
          base = 10,
          thresholds = List(15 -> ExpMultiplier.Condition.CombatLevel(71))
        ),
        ExpMultiplier(
          Skill.nonCombats,
          ExpMultiplier.Kind.Multiplicative,
          base = 10,
          thresholds = List.empty
        )
      ),
      maybeLeaguePointScoring = None
    )
}
