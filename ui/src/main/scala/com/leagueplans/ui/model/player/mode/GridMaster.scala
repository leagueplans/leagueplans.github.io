package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.Skill.{Herblore, Hitpoints}
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.{ExpMultiplier, Plan}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Level, Stats}

object GridMaster extends Mode {
  val name: String = "Grid Master"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        stats = Stats(
          Herblore -> Level(3).bound,
          Hitpoints -> Level(10).bound
        ),
        depositories = MainGame.initialPlayer.depositories + (
          Depository.Kind.Inventory -> Depository(
            MainGame.initialInventory + ((Item.ID(3737), false) -> 1), // Dramen staff
            Depository.Kind.Inventory
          )
        ),
        completedQuests = Set(
          3, // The Restless Ghost
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
          186 // Children of the Sun
        )
      ),
      expMultipliers = List(
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 4,
          thresholds = List.empty
        ),
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 0,
          thresholds = List(2 -> ExpMultiplier.Condition.GridAxis(ExpMultiplier.GridAxisDirection.Row, 4))
        ),
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 0,
          thresholds = List(2 -> ExpMultiplier.Condition.GridAxis(ExpMultiplier.GridAxisDirection.Column, 4))
        ),
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 0,
          thresholds = List(2 -> ExpMultiplier.Condition.GridTile(16)) // Obtain a rare unique from the Barrows chest while wearing some Barrows equipment
        ),
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 0,
          thresholds = List(2 -> ExpMultiplier.Condition.GridTile(9)) // Subdue the Moons of Peril
        ),
        ExpMultiplier(
          Skill.values.toSet,
          ExpMultiplier.Kind.Additive,
          base = 0,
          thresholds = List(2 -> ExpMultiplier.Condition.GridTile(48)) // Obtain a rare drop from Araxxor
        ),
      ),
      maybeLeaguePointScoring = None
    )
}
