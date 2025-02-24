package com.leagueplans.ui.dom.planning.player

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.player.view.{CharacterTab, LeagueTab, QuestAndDiaryTab, View}
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplierStrategy}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

object Visualiser {
  def apply(
    playerSignal: Signal[Player],
    expMultiplierStrategySignal: Signal[ExpMultiplierStrategy],
    isLeague: Boolean,
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val characterTab = View.Tab("Character", CharacterTab(playerSignal, expMultiplierStrategySignal, cache, itemFuse, effectObserverSignal, contextMenuController, modalController, toastPublisher))
    val questTab = View.Tab("Quests & Diaries", QuestAndDiaryTab(playerSignal, cache, effectObserverSignal, contextMenuController))

    if (isLeague)
      View(
        characterTab,
        questTab,
        View.Tab("League progress", LeagueTab(playerSignal, cache, effectObserverSignal, contextMenuController))
      )
    else
      View(characterTab, questTab)
  }
}
