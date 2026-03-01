package com.leagueplans.ui.dom.planning.player

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.player.view.{CharacterTab, GridTab, LeagueTab, QuestAndDiaryTab, View}
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

object Visualiser {
  def apply(
    playerSignal: Signal[Player],
    isLeague: Boolean,
    isGridMaster: Boolean,
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    expMultipliers: List[ExpMultiplier],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val characterTab = View.Tab("Character", CharacterTab(playerSignal, cache, itemFuse, effectObserverSignal, expMultipliers, tooltip, contextMenuController, modal, toastPublisher))
    val questTab = View.Tab("Quests & Diaries", QuestAndDiaryTab(playerSignal, cache, effectObserverSignal, tooltip, contextMenuController))

    if (isLeague)
      View(
        characterTab,
        questTab,
        View.Tab("League progress", LeagueTab(playerSignal, cache, effectObserverSignal, tooltip, contextMenuController))
      )
    else if (isGridMaster)
      View(
        characterTab,
        questTab,
        View.Tab("Grid progress", GridTab(playerSignal, cache, effectObserverSignal))
      )
    else
      View(characterTab, questTab)
  }
}
