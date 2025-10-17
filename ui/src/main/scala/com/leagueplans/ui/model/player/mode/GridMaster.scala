package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplier, Plan}
import com.leagueplans.ui.model.player.item.Depository

object GridMaster extends Mode {
  val name: String = "Grid Master"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        depositories = MainGame.initialPlayer.depositories + (
          Depository.Kind.Inventory -> Depository(
            MainGame.initialInventory + ((Item.ID(3737), false) -> 1), // Dramen staff
            Depository.Kind.Inventory
          )
        ),
        completedQuests = Set(
          10, // Prince Ali Rescue
          18, // Druidic Ritual
          19, // Lost City
          40, // Jungle Potion
          42, // Shilo Village
          45, // The Tourist Trap
          49, // The Dig Site
          50, // Gertrude's Cat
          53, // Rune Mysteries
          56, // Priest in Peril
          57, // Nature Spirit
          58, // Death Plateau
          60, // Tai Bwo Wannai Trio
          80, // Icthlarin's Little Helper
          105, // Fairytale I - Growing Pains
          122, // Fairytale II - Cure a Queen
          132, // Contact!
          147, // Client of Kourend
          152, // Bone Voyage
          153, // The Queen of Thieves
          154, // The Depths of Despair
          156, // Corsair Curse
          158, // Tale of the Righteous
          161, // The Forsaken Tower
          162, // The Ascent of Arceuus
          163, // X Marks the Spot
          173, // A Kingdom Divided
          178, // Beneath Cursed Sands
          198 // Children of the Sun
        )
      ),
      expMultipliers = List(ExpMultiplier(Skill.values.toSet, base = 5, thresholds = List.empty)),
      maybeLeaguePointScoring = None
    )
}
