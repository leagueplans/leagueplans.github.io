package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.item.bank.BankElement
import com.leagueplans.ui.dom.planning.player.item.equipment.EquipmentElement
import com.leagueplans.ui.dom.planning.player.item.inventory.InventoryElement
import com.leagueplans.ui.model.player.item.Depository
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemsSection {
  def apply(ctx: SectionContext): L.Div =
    L.div(
      L.cls(Styles.section),
      EquipmentElement(
        ctx.displayedPlayer,
        ctx.cache,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu
      ),
      InventoryElement(
        ctx.displayedPlayer,
        ctx.cache,
        ctx.itemFuse,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu,
        ctx.modal,
        ctx.toasts
      ),
      BankElement(
        ctx.displayedPlayer.map(_.get(Depository.Kind.Bank)),
        ctx.cache,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu,
        ctx.modal
      ).amend(L.cls(Styles.bank))
    )

  @js.native @JSImport("/styles/planning/section/itemsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val bank: String = js.native
  }
}
