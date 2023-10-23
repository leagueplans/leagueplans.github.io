package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.view.{CharacterTab, QuestAndDiaryTab, View}
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
  ): L.Div = {
    val randomStats =
      L.child <-- playerSignal.map(p =>
        KeyValuePairs(
          L.span("Multiplier:") -> L.span(p.leagueStatus.multiplier),
          L.span("Tasks completed:") -> L.span(p.leagueStatus.tasksCompleted.size),
          L.span("League points:") -> L.span(p.leagueStatus.leaguePoints)
        )
      )

    View(
      View.Tab("Character", CharacterTab(playerSignal, cache, itemFuse, effectObserverSignal, contextMenuController, modalBus)),
      View.Tab("Quests & Diaries", QuestAndDiaryTab(playerSignal, cache, effectObserverSignal, contextMenuController)),
      View.Tab("[Under construction] League progress", L.div(randomStats))
    )
  }
}
