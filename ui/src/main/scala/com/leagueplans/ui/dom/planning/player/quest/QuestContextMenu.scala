package com.leagueplans.ui.dom.planning.player.quest

import com.leagueplans.ui.dom.common.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Effect.CompleteQuest
import com.leagueplans.ui.model.player.Quest
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object QuestContextMenu {
  def apply(
    quest: Quest,
    effectObserver: Observer[CompleteQuest],
    contextMenu: ContextMenu
  ): L.Div =
    ContextMenuList(
      ContextMenuList.Item(
        FontAwesome.icon(FreeSolid.faCheck),
        "Complete",
        Button(
          _.handledAs[CompleteQuest](CompleteQuest(quest.id)) --> 
            Observer.combine(effectObserver, Observer(_ => contextMenu.close()))
        )
      )
    )
}
