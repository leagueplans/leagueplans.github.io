package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.item.bank.{BankElement, BankFooter}
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.dom.planning.player.item.equipment.EquipmentElement
import com.leagueplans.ui.dom.planning.player.item.inventory.panel.InventoryPanel
import com.leagueplans.ui.model.player.item.ItemActions
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Equipment and the inventory sit on the left at their in-game sizes, and the bank takes the
  * rest of the space, scrolling on its own. Clicking any stack opens its card.
  *
  * Stacks can be dragged between the panels, and shift-clicked to move a whole stack.
  *
  * Drafts: the bank search and the bank's transfer settings don't depend on the focused step, so
  * they're kept when the focus changes. An open card does, so it closes.
  */
object ItemsSection {
  def apply(ctx: SectionContext): L.Div = {
    val bankQuery = Var("")
    var section = Option.empty[Element]

    val itemDrag =
      ItemDrag(
        ctx.dragSession,
        ctx.playerAtInsertion,
        ctx.effectObserver,
        ctx.isRecalculating,
        ctx.cache.items,
        ctx.undoToasts,
        ctx.tooltip,
        ctx.popover
      )

    val itemCards =
      ItemCards(
        ctx.popover,
        ctx.playerAtInsertion,
        ctx.effectObserver,
        ctx.requirementObserver,
        ctx.cache,
        itemDrag.settings.signal,
        ctx.undoToasts,
        ctx.tooltip,
        // Cards prefer to stay over the section, so that the plan and step details stay visible
        boundary = () => section
      )

    val depositInventory = ItemActions.depositInventory(_, ctx.cache.items)

    L.div(
      L.cls(Styles.section),
      L.onMountUnmountCallback(ctx => section = Some(ctx.thisNode.ref), _ => section = None),
      EquipmentElement(
        ctx.displayedPlayer,
        ctx.cache,
        itemCards,
        itemDrag,
        ctx.tooltip
      ).amend(L.cls(Styles.equipment)),
      InventoryPanel(
        ctx.displayedPlayer,
        bankQuery.signal,
        ctx.cache,
        itemCards,
        itemDrag,
        ctx.tooltip,
        ctx.toasts
      ).amend(L.cls(Styles.inventory)),
      BankElement(
        ctx.displayedPlayer,
        ctx.playerAtInsertion,
        bankQuery,
        ctx.cache,
        itemCards,
        itemDrag,
        ctx.tooltip,
        slotsUsed => BankFooter(
          slotsUsed,
          itemDrag.settings,
          ctx.playerAtInsertion,
          ctx.effectObserver,
          depositInventory,
          ItemActions.depositEquipment(_, ctx.cache.items),
          ctx.undoToasts,
          ctx.tooltip
        )
      ).amend(L.cls(Styles.bank))
    )
  }

  @js.native @JSImport("/styles/planning/section/itemsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val equipment: String = js.native
    val inventory: String = js.native
    val bank: String = js.native
  }
}
