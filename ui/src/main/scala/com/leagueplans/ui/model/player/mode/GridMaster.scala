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
          19, // Lost city
          56, // Priest in peril
          57, // Nature spirit
          105, // Fairytale I - growing pains
          122, // Fairytale II - cure a queen
          156, // Corsair curse
          198 // Children of the Sun
        )
      ),
      expMultipliers = List(ExpMultiplier(Skill.values.toSet, base = 5, thresholds = List.empty)),
      maybeLeaguePointScoring = None
    )
}
