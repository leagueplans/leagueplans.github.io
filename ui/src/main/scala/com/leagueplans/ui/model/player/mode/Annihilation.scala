package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplier, Plan}
import com.leagueplans.ui.model.player.item.Depository

object Annihilation extends Mode.Deadman {
  val name: String = "Deadman: Annihilation"

  val settings: Plan.Settings.Explicit = {
    Armageddon.settings.copy(
      initialPlayer = Armageddon.settings.initialPlayer.copy(
        depositories = List(
          Depository(
            Map(
              // TODO Deadman's skull
              (Item.ID(3262), false) -> 1 // Deadman starter pack
            ),
            Depository.Kind.Inventory
          ),
          Depository(
            MainGame.initialInventory ++
              MainGame.initialBank +
              ((Item.ID(10670), false) -> 30), // Tuna (Deadman starter pack)
            Depository.Kind.Bank
          )
        ).map(d => d.kind -> d).toMap,
        completedQuests = Set(
          2, // Demon Slayer
          3, // The Restless Ghost
          6, // Shield of Arrav
          7, // Ernest the Chicken
          8, // Vampyre Slayer
          15, // Goblin Diplomacy
          18, // Druidic Ritual
          19, // Lost City
          20, // Witch's House
          21, // Merlin's Crystal
          23, // Alfred Grimhand's Barcrawl
          31, // Holy Grail
          32, // Tree Gnome Village
          38, // Waterfall Quest
          41, // The Grand Tree
          53, // Rune Mysteries
          56, // Priest in Peril
          57, // Nature Spirit
          65, // Horror from the Deep
          67, // Monkey Madness I
          105, // Fairytale I - Growing Pains
          122, // Fairytale II - Cure a Queen
          131, // Animal Magnetism
          147, // Client of Kourend
          153, // The Queen of Thieves
          154, // The Depths of Despair
          158, // Tale of the Righteous
          161, // The Forsaken Tower
          162, // The Ascent of Arceuus
          163, // X Marks the Spot
          186, // Children of the Sun
        ),
        completedDiaryTasks = Set(
          246, // Hand in a book at the Arceuus Library
          292, // Pickpocket a man or woman in Lumbridge
        )
      ),
      expMultipliers = List(
        ExpMultiplier(
          Skill.combats,
          ExpMultiplier.Kind.Multiplicative,
          base = 10,
          thresholds = List(
            15 -> ExpMultiplier.Condition.CombatLevel(61),
            20 -> ExpMultiplier.Condition.CombatLevel(96)
          )
        ),
        ExpMultiplier(
          Skill.nonCombats,
          ExpMultiplier.Kind.Multiplicative,
          base = 10,
          thresholds = List.empty
        )
      )
    )
  }
}
