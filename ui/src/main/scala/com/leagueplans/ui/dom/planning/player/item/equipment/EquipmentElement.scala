package com.leagueplans.ui.dom.planning.player.item.equipment

import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.model.player.item.ItemTransfer
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Worn items, in a panel styled like the inventory's. The slots keep their in-game places, joined
  * by the game's bars.
  */
object EquipmentElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      // Dropping anywhere on the panel wears the item in its own slot
      itemDrag.target(ItemTransfer.Target.Worn),
      header,
      L.div(
        L.cls(Styles.layout),
        // The slots cover the bars, so the bars only show between slots
        L.div(L.cls(Styles.spine)),
        L.div(L.cls(Styles.weaponBar)),
        L.div(L.cls(Styles.shieldBar)),
        L.div(L.cls(Styles.neckBar)),
        L.div(L.cls(Styles.bodyBar)),
        L.children <-- playerSignal.map(player =>
          EquipmentSlot.values.toList.map(slot =>
            EquipmentSlotElement(
              slot,
              cache.itemise(player.get(slot)),
              itemCards,
              itemDrag,
              tooltip
            ).amend(L.cls(toStyle(slot)))
          )
        )
      )
    )

  private def header: L.Element =
    L.headerTag(
      L.cls(DepositoryStyles.header, PanelStyles.header),
      L.img(
        L.cls(Styles.icon, DepositoryStyles.icon),
        L.src(icon),
        L.alt("Equipment icon")
      ),
      "Equipment"
    )

  @js.native @JSImport("/images/equipment-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/equipment/equipmentElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val layout: String = js.native

    val spine: String = js.native
    val weaponBar: String = js.native
    val shieldBar: String = js.native
    val neckBar: String = js.native
    val bodyBar: String = js.native

    val headSlot: String = js.native
    val capeSlot: String = js.native
    val neckSlot: String = js.native
    val ammoSlot: String = js.native
    val weaponSlot: String = js.native
    val shieldSlot: String = js.native
    val bodySlot: String = js.native
    val legsSlot: String = js.native
    val handsSlot: String = js.native
    val feetSlot: String = js.native
    val ringSlot: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toStyle(slot: EquipmentSlot): String =
    slot match {
      case EquipmentSlot.Head => Styles.headSlot
      case EquipmentSlot.Cape => Styles.capeSlot
      case EquipmentSlot.Neck => Styles.neckSlot
      case EquipmentSlot.Ammo => Styles.ammoSlot
      case EquipmentSlot.Weapon => Styles.weaponSlot
      case EquipmentSlot.Shield => Styles.shieldSlot
      case EquipmentSlot.Body => Styles.bodySlot
      case EquipmentSlot.Legs => Styles.legsSlot
      case EquipmentSlot.Hands => Styles.handsSlot
      case EquipmentSlot.Feet => Styles.feetSlot
      case EquipmentSlot.Ring => Styles.ringSlot
    }
}
