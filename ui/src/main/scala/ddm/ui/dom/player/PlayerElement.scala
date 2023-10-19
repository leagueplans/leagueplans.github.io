package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.diary.DiaryPanel
import ddm.ui.dom.player.item.bank.BankElement
import ddm.ui.dom.player.item.equipment.EquipmentElement
import ddm.ui.dom.player.item.inventory.InventoryElement
import ddm.ui.dom.player.quest.QuestList
import ddm.ui.dom.player.stats.StatsElement
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

object PlayerElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div = {
    L.div(
      L.div(
        L.display.flex,
        StatsElement.from(playerSignal, effectObserverSignal, contextMenuController),
        EquipmentElement(playerSignal, cache, effectObserverSignal, contextMenuController),
        InventoryElement(playerSignal, cache, itemFuse, effectObserverSignal, contextMenuController, modalBus),
        BankElement(
          playerSignal.map(_.get(Depository.Kind.Bank)),
          cache,
          effectObserverSignal,
          contextMenuController,
          modalBus
        )
      ),
      L.div(
        L.display.flex,
        L.child <-- playerSignal.map(p =>
          KeyValuePairs(
            L.span("Combat level:") -> L.span(String.format("%.2f", p.stats.combatLevel)),
            L.span("Multiplier:") -> L.span(p.leagueStatus.multiplier),
            L.span("Tasks completed:") -> L.span(p.leagueStatus.tasksCompleted.size),
            L.span("League points:") -> L.span(p.leagueStatus.leaguePoints),
            L.span("Expected renown:") -> L.span(p.leagueStatus.expectedRenown)
          )
        ),
        QuestList(playerSignal, cache, effectObserverSignal, contextMenuController),
        DiaryPanel(playerSignal, cache, effectObserverSignal, contextMenuController),
      )
    )
  }
}
