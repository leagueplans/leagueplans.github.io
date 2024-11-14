package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.common.*
import ddm.ui.dom.player.view.{CharacterTab, LeagueTab, QuestAndDiaryTab, View}
import ddm.ui.model.plan.{Effect, ExpMultiplierStrategy}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

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
