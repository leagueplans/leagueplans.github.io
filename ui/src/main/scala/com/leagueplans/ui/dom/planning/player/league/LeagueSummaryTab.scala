package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.ui.dom.planning.player.task.TaskSummaryTab
import com.leagueplans.ui.model.player.mode.Mode
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object LeagueSummaryTab {
  def apply(leagueObserver: Observer[Mode.League]): L.Div =
    TaskSummaryTab(
      Mode.League.all.map(league =>
        LeagueOption(league, leagueObserver.contramap[Unit](_ => league))
      )
    )
}
