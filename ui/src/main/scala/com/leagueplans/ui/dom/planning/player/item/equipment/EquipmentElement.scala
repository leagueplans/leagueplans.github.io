package com.leagueplans.ui.dom.planning.player.item.equipment

import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.model.player.item.ItemTransfer
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EquipmentElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.pane),
      // Dropping anywhere on the panel wears the item in its own slot
      itemDrag.target(ItemTransfer.Target.Worn),
      L.img(
        L.cls(Styles.background),
        L.src(background)
      ),
      L.inContext(panel =>
        L.children <-- playerSignal.map(player =>
          EquipmentSlot.values.toList.map(slot =>
            EquipmentSlotElement(
              slot,
              cache.itemise(player.get(slot)),
              itemCards,
              itemDrag,
              panel,
              tooltip
            ).amend(L.cls(toStyle(slot)))
          )
        )
      )
    )

  @js.native @JSImport("/images/equipment/pane.png", JSImport.Default)
  private val background: String = js.native

  @js.native @JSImport("/styles/planning/player/item/equipment/equipmentElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val background: String = js.native

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
