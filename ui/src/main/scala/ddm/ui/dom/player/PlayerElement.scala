package ddm.ui.dom.player

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, intToNode, seqToModifier, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.item.{DepositoryElement, EquipmentElement}
import ddm.ui.dom.player.stats.StatsElement
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse

object PlayerElement {
  def apply(
    player: Signal[Player],
    itemCache: ItemCache,
    items: Fuse[Item],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div = {
    L.div(
      EquipmentElement(player, itemCache, items, effectObserver, contextMenuController, modalBus),
      L.div(
        L.display.flex,
        StatsElement.from(player, effectObserver, contextMenuController),
        List(Depository.Kind.Inventory, Depository.Kind.Bank).map(kind =>
          DepositoryElement(player.map(_.get(kind)), itemCache, items, effectObserver, contextMenuController, modalBus)
        )
      ),
      L.child <-- player.map(p =>
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
