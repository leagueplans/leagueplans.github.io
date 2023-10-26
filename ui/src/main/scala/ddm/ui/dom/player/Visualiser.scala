package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.view.{CharacterTab, LeagueTab, QuestAndDiaryTab, View}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

object Visualiser {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div =
    View(
      View.Tab("Character", CharacterTab(playerSignal, cache, itemFuse, effectObserverSignal, contextMenuController, modalBus)),
      View.Tab("Quests & Diaries", QuestAndDiaryTab(playerSignal, cache, effectObserverSignal, contextMenuController)),
      View.Tab("League progress", LeagueTab(playerSignal, cache, effectObserverSignal, contextMenuController))
    )
}
