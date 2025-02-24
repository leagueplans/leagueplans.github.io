package com.leagueplans.ui.dom.planning.player.view

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.player.MultiplierElement
import com.leagueplans.ui.dom.planning.player.item.bank.BankElement
import com.leagueplans.ui.dom.planning.player.item.equipment.EquipmentElement
import com.leagueplans.ui.dom.planning.player.item.inventory.InventoryElement
import com.leagueplans.ui.dom.planning.player.stats.StatsElement
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplierStrategy}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object CharacterTab {
  def apply(
    playerSignal: Signal[Player],
    expMultiplierStrategySignal: Signal[ExpMultiplierStrategy],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
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
        modalController,
        toastPublisher
      ).amend(L.cls(Styles.inventoryPanel)),
      BankElement(
        playerSignal.map(_.get(Depository.Kind.Bank)),
        cache,
        effectObserverSignal,
        contextMenuController,
        modalController
      ).amend(L.cls(Styles.bankPanel)),
      StatsElement.from(
        playerSignal,
        effectObserverSignal,
        contextMenuController,
        modalController
      ).amend(L.cls(Styles.statsPanel)),
      MultiplierElement(
        expMultiplierStrategySignal,
        playerSignal.map(_.leagueStatus.leaguePoints),
      ).amend(L.cls(Styles.multiplierPanel))
    )

  @js.native @JSImport("/styles/planning/player/view/characterTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val equipmentPanel: String = js.native
    val inventoryPanel: String = js.native
    val bankPanel: String = js.native
    val statsPanel: String = js.native
    val multiplierPanel: String = js.native
  }
}
