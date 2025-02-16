package com.leagueplans.ui.dom.player.league

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.leagueplans.ui.utils.laminar.LaminarOps.handledAs
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

object LeagueTaskContextMenu {
  def apply(
    leagueTask: LeagueTask,
    effectObserver: Observer[CompleteLeagueTask],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      _.handledAs[CompleteLeagueTask](CompleteLeagueTask(leagueTask.id)) -->
        Observer.combine(effectObserver, menuCloser)
    ).amend("Complete")
}
