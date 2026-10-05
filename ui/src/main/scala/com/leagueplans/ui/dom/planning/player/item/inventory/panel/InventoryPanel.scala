package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.dom.planning.player.item.{DepositoryStacks, ItemQuery, StackElement}
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.item.{Depository, ItemStack, ItemTransfer}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
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
    tooltip: Tooltip
  ): L.Div = {
    val stacks = playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory)))

    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      itemDrag.target(ItemTransfer.Target.Inventory),
      InventoryHeader(used = stacks.map(_.size)),
      L.inContext(panel =>
        DepositoryStacks(
          stacks,
          columnCount = 4,
          rowCount = 7,
          overflowRowCount = 20,
          toStackElement(query, itemCards, itemDrag, panel, tooltip),
          tooltip
        ).amend(L.cls(Styles.contents))
      )
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
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
    panel: L.HtmlElement,
    tooltip: Tooltip
  )(stack: ItemStack): L.Div =
    StackElement(
      stack,
      tooltip,
      tooltipConfig = FloatingConfig.basicAnchoredTooltip(anchor = panel, Placement.bottom, offset = 2),
      hideTooltip = itemCards.isOpenOn
    ).amend(
      L.cls(Styles.unmatched) <-- query.map(query =>
        !ItemQuery.isEmpty(query) && !ItemQuery.matches(stack.item, query)
      ),
      itemCards.trigger(Holding(stack.item, stack.noted, Depository.Kind.Inventory)),
      itemDrag.source(Holding(stack.item, stack.noted, Depository.Kind.Inventory))
    )
}
