package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.item.bank.BankElement
import com.leagueplans.ui.dom.planning.player.item.equipment.EquipmentElement
import com.leagueplans.ui.dom.planning.player.item.inventory.InventoryElement
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Worn items and the inventory sit on the left at their in-game sizes, and the bank takes the
  * rest of the space, scrolling on its own. */
object ItemsSection {
  def apply(ctx: SectionContext): L.Div = {
    // Doesn't depend on the focused step, so it's kept when the focus changes
    val bankQuery = Var("")

    L.div(
      L.cls(Styles.section),
      EquipmentElement(
        ctx.displayedPlayer,
        ctx.cache,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu
      ).amend(L.cls(Styles.worn)),
      InventoryElement(
        ctx.displayedPlayer,
        bankQuery.signal,
        ctx.cache,
        ctx.itemFuse,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu,
        ctx.modal,
        ctx.toasts
      ).amend(L.cls(Styles.inventory)),
      BankElement(
        ctx.displayedPlayer,
        bankQuery,
        ctx.cache,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu,
        ctx.modal
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
