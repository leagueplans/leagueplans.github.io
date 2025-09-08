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
          3, // The restless ghost
          6, // Shield of Arrav
          7, // Ernest the chicken
          15, // Goblin diplomacy
          18, // Druidic ritual
          19, // Lost city
          21, // Merlin's crystal
          23, // Alfred Grimhand's barcrawl
          31, // Holy grail
          53, // Rune mysteries
          56, // Priest in peril
          57, // Nature spirit
          105, // Fairytale I - growing pains
          122, // Fairytale II - cure a queen
          131 // Animal magnetism
        ),
        completedDiaryTasks = Set(
          193 // Read the blackboard at Barbarian Assault after reaching level 5 in every role
        )
      ),
      expMultipliers = List(
        ExpMultiplier(
          Skill.combats,
          base = 10,
          thresholds = List(15 -> ExpMultiplier.Condition.CombatLevel(71))
        ),
        ExpMultiplier(
          Skill.nonCombats,
          base = 10,
          thresholds = List.empty
        )
      ),
      maybeLeaguePointScoring = None
    )
}
