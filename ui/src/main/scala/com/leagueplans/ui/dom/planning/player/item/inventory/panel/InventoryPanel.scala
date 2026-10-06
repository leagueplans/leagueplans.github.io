package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.leagueplans.ui.dom.planning.player.item.DepositoryStacks.Layout
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.dom.planning.player.item.{DepositoryStacks, ItemQuery, StackElement}
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.item.{Depository, ItemStack, ItemTransfer}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{ToastHub, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryPanel {
  /** @param query the bank search. Stacks that don't match it fade out. */
  def apply(
    playerSignal: Signal[Player],
    query: Signal[String],
    cache: Cache,
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip,
    toasts: ToastHub.Publisher
  ): L.Div = {
    val stacks = playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory)))
    val numbered = stacks.map(DepositoryStacks.numbered)
    val capacity = Depository.Kind.Inventory.capacity
    val toElement = toStackElement(query, itemCards, itemDrag, tooltip)

    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      itemDrag.target(ItemTransfer.Target.Inventory),
      InventoryHeader(),
      // Scrolls as the bank does when the section is too short for the whole inventory, with any
      // stacks that don't fit beneath the rest
      L.div(
        L.cls(Styles.scroller),
        DepositoryStacks.within(numbered, capacity, Layout.Columns(4, rows = Some(7)), toElement),
        L.child.maybe <-- DepositoryStacks.beyond(numbered, capacity, renderLimit = 80, layout = Layout.Columns(4), toElement = toElement)
      ),
      InventoryFooter(stacks, toasts, tooltip)
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val scroller: String = js.native
    val unmatched: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
  }

  private def toStackElement(
    query: Signal[String],
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip
  )(stack: ItemStack): L.Div =
    StackElement(
      stack,
      tooltip,
      // Beside the stack, as the bank's are
      tooltipConfig = FloatingConfig.basicTooltip(Placement.bottom, offset = 6),
      hideTooltip = itemCards.isOpenOn
    ).amend(
      L.cls(Styles.unmatched) <-- query.map(query =>
        !ItemQuery.isEmpty(query) && !ItemQuery.matches(stack.item, query)
      ),
      itemCards.trigger(Holding(stack.item, stack.noted, Depository.Kind.Inventory)),
      itemDrag.source(Holding(stack.item, stack.noted, Depository.Kind.Inventory))
    )
}
