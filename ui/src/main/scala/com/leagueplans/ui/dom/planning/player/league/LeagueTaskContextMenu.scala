package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.uicommon.utils.laminar.FontAwesome
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
