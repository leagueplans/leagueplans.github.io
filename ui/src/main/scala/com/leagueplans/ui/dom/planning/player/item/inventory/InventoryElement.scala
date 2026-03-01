package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.planning.player.item.inventory.panel.InventoryPanel
import com.leagueplans.ui.dom.planning.player.item.inventory.sidebar.InventorySidebar
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val panel = InventoryPanel(
      playerSignal,
      cache,
      effectObserverSignal,
      tooltip,
      contextMenuController,
      modal
    )
    val sidebar = InventorySidebar(
      playerSignal,
      cache,
      itemFuse,
      effectObserverSignal,
      tooltip,
      modal,
      toastPublisher
    )
    L.div(
      L.cls(Styles.element),
      panel.amend(L.cls(Styles.panel)),
      sidebar.amend(L.cls(Styles.sidebar))
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val element: String = js.native
    val panel: String = js.native
    val sidebar: String = js.native
  }
}
