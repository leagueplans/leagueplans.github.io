package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryHeader {
  def apply(): L.Element =
    L.headerTag(
      L.cls(Styles.header, DepositoryStyles.header, PanelStyles.header),
      L.img(
        L.cls(Styles.inventoryIcon, DepositoryStyles.icon),
        L.src(icon),
        L.alt("Inventory icon")
      ),
      "Inventory"
    )

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val inventoryIcon: String = js.native
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
