package com.leagueplans.ui.model.player.mode

import com.leagueplans.common.model.LeagueTaskTier
import com.leagueplans.ui.model.plan.{LeaguePointScoring, Plan}

object LeaguesVI extends Mode.League {
  val name: String = "Leagues VI: Demonic Pacts"

  // TODO Change task validation to check that a task is part of this league
  val settings: Plan.Settings.Explicit =
    LeaguesV.settings.copy(
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
