package ddm.ui.dom.player.view

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.common._
import ddm.ui.dom.player.item.bank.BankElement
import ddm.ui.dom.player.item.equipment.EquipmentElement
import ddm.ui.dom.player.item.inventory.InventoryElement
import ddm.ui.dom.player.stats.StatsElement
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object CharacterTab {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      EquipmentElement(
        playerSignal,
        cache,
        effectObserverSignal,
        contextMenuController
      ).amend(L.cls(Styles.equipmentPanel)),
      InventoryElement(
        playerSignal,
        cache,
        itemFuse,
        effectObserverSignal,
        contextMenuController,
        modalBus
      ).amend(L.cls(Styles.inventoryPanel)),
      BankElement(
        playerSignal.map(_.get(Depository.Kind.Bank)),
        cache,
        effectObserverSignal,
        contextMenuController,
        modalBus
      ).amend(L.cls(Styles.bankPanel)),
      StatsElement.from(
        playerSignal,
        effectObserverSignal,
        contextMenuController
      ).amend(L.cls(Styles.statsPanel)),
    )

  @js.native @JSImport("/styles/player/view/characterTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val equipmentPanel: String = js.native
    val inventoryPanel: String = js.native
    val bankPanel: String = js.native
    val statsPanel: String = js.native
  }
}
