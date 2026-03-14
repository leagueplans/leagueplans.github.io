package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object LeagueTaskContextMenu {
  def apply(
    leagueTask: LeagueTask,
    effectObserver: Observer[CompleteLeagueTask],
    contextMenu: ContextMenu
  ): L.Div =
    ContextMenuList(
      ContextMenuList.Item(
        FontAwesome.icon(FreeSolid.faCheck),
        "Complete",
        Button(
          _.handledAs[CompleteLeagueTask](CompleteLeagueTask(leagueTask.id)) -->
            Observer.combine(effectObserver, Observer(_ => contextMenu.close()))
        )
      )
    )
}
