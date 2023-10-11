package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, intToNode, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.item.bank.BankElement
import ddm.ui.dom.player.item.equipment.EquipmentElement
import ddm.ui.dom.player.item.inventory.InventoryElement
import ddm.ui.dom.player.stats.StatsElement
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse

object PlayerElement {
  def apply(
    playerSignal: Signal[Player],
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div = {
    L.div(
      L.div(
        L.display.flex,
        StatsElement.from(playerSignal, effectObserverSignal, contextMenuController),
        EquipmentElement(playerSignal, itemCache, effectObserverSignal, contextMenuController),
        InventoryElement(playerSignal, itemCache, itemFuse, effectObserverSignal, contextMenuController, modalBus),
        BankElement(
          playerSignal.map(_.get(Depository.Kind.Bank)),
          itemCache,
          effectObserverSignal,
          contextMenuController,
          modalBus
        )
      ),
      L.child <-- playerSignal.map(p =>
        KeyValuePairs(
          L.span("Quest points:") -> L.span(p.questPoints),
          L.span("Combat level:") -> L.span(String.format("%.2f", p.stats.combatLevel)),
          L.span("Multiplier:") -> L.span(p.leagueStatus.multiplier),
          L.span("Tasks completed:") -> L.span(p.leagueStatus.tasksCompleted.size),
          L.span("League points:") -> L.span(p.leagueStatus.leaguePoints),
          L.span("Expected renown:") -> L.span(p.leagueStatus.expectedRenown)
        )
      )
    )
  }
}
