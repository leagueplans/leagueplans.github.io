package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{ExpMultiplier, Plan}

object GridMaster extends Mode {
  val name: String = "Grid Master"

  val settings: Plan.Settings.Explicit =
    Plan.Settings.Explicit(
      initialPlayer = MainGame.initialPlayer.copy(
        completedQuests = Set(
          156 // Corsair curse
        )
      ),
      expMultipliers = List(ExpMultiplier(Skill.values.toSet, base = 4, thresholds = List.empty)),
      maybeLeaguePointScoring = None
    )
}
