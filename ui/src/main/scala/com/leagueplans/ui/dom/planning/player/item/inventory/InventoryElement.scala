package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.inventory.panel.InventoryPanel
import com.leagueplans.ui.dom.planning.player.item.inventory.sidebar.InventorySidebar
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{Modal, ToastHub, Tooltip}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    query: Signal[String],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect | Seq[Effect]]]],
    itemCards: ItemCards,
    tooltip: Tooltip,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val panel = InventoryPanel(
      playerSignal,
      query,
      cache,
      itemCards,
      tooltip
    )
    val sidebar = InventorySidebar(
      playerSignal,
      cache,
      effectObserverSignal,
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
