package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.common.*
import ddm.ui.dom.player.view.{CharacterTab, LeagueTab, QuestAndDiaryTab, View}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.league.ExpMultiplierStrategy
import ddm.ui.model.player.mode.Mode
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

object Visualiser {
  def apply(
    playerSignal: Signal[Player],
    mode: Mode,
    cache: Cache,
    itemFuse: Fuse[Item],
    expMultiplierStrategyObserver: Observer[ExpMultiplierStrategy],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  ): L.Div = {
    val characterTab = View.Tab("Character", CharacterTab(playerSignal, cache, itemFuse, expMultiplierStrategyObserver, effectObserverSignal, contextMenuController, modalController))
    val questTab = View.Tab("Quests & Diaries", QuestAndDiaryTab(playerSignal, cache, effectObserverSignal, contextMenuController))

    mode match {
      case _: Mode.League =>
        View(
          characterTab,
          questTab,
          View.Tab("League progress", LeagueTab(playerSignal, cache, effectObserverSignal, contextMenuController))
        )
      case _ =>
        View(characterTab, questTab)
    }
  }
}
