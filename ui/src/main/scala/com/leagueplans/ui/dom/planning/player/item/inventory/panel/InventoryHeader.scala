package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.leagueplans.ui.model.player.item.Depository
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryHeader {
  /** @param used how many of the inventory's slots are taken */
  def apply(used: Signal[Int]): L.Element =
    L.headerTag(
      L.cls(Styles.header, DepositoryStyles.header, PanelStyles.header),
      L.img(
        L.cls(Styles.inventoryIcon, DepositoryStyles.icon),
        L.src(icon),
        L.alt("Inventory icon")
      ),
      "Inventory",
      L.span(L.cls(Styles.used), L.text <-- used.map(used => s"$used/${Depository.Kind.Inventory.capacity}"))
    )

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val inventoryIcon: String = js.native
    val used: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }
}
