package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.item.bank.BankElement
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.equipment.EquipmentElement
import com.leagueplans.ui.dom.planning.player.item.inventory.InventoryElement
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Worn items and the inventory sit on the left at their in-game sizes, and the bank takes the
  * rest of the space, scrolling on its own. Clicking any stack opens its card.
  *
  * Drafts: the bank search doesn't depend on the focused step, so it's kept when the focus
  * changes. An open card does, so it closes.
  */
object ItemsSection {
  def apply(ctx: SectionContext): L.Div = {
    val bankQuery = Var("")
    var section = Option.empty[Element]

    val itemCards =
      ItemCards(
        ctx.popover,
        ctx.playerAtInsertion,
        ctx.effectObserver,
        ctx.cache,
        ctx.undoToasts,
        ctx.tooltip,
        // Cards prefer to stay over the section, so that the plan and step details stay visible
        boundary = () => section
      )

    L.div(
      L.cls(Styles.section),
      L.onMountUnmountCallback(ctx => section = Some(ctx.thisNode.ref), _ => section = None),
      EquipmentElement(
        ctx.displayedPlayer,
        ctx.cache,
        itemCards,
        ctx.tooltip
      ).amend(L.cls(Styles.worn)),
      InventoryElement(
        ctx.displayedPlayer,
        bankQuery.signal,
        ctx.cache,
        ctx.itemFuse,
        ctx.effectObserver,
        itemCards,
        ctx.tooltip,
        ctx.modal,
        ctx.toasts
      ).amend(L.cls(Styles.inventory)),
      BankElement(
        ctx.displayedPlayer,
        bankQuery,
        ctx.cache,
        itemCards,
        ctx.tooltip
      ).amend(L.cls(Styles.bank))
    )
  }

  @js.native @JSImport("/styles/planning/section/itemsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val worn: String = js.native
    val inventory: String = js.native
    val bank: String = js.native
  }
}
