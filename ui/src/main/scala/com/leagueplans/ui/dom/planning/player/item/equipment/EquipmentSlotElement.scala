package com.leagueplans.ui.dom.planning.player.item.equipment

import com.leagueplans.ui.dom.planning.player.item.{StackElement, StackIcon}
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.laminar.api.{L, nodeOptionToModifier, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EquipmentSlotElement {
  def apply(
    slot: EquipmentSlot,
    stacks: List[ItemStack],
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip
  ): L.Div =
    L.div(
      L.cls(Styles.slot),
      L.cls(ItemDrag.Styles.slotTarget) <-- itemDrag.slotHighlights.map(_.get(slot).contains(ItemDrag.SlotHighlight.Target)),
      L.cls(ItemDrag.Styles.slotDisplaced) <-- itemDrag.slotHighlights.map(_.get(slot).contains(ItemDrag.SlotHighlight.Displaced)),
      L.img(
        L.cls(Styles.background),
        L.src(toBackground(slot, stacks.isEmpty)),
        L.alt(slot.name)
      ),
      // A slot holding more than one item, which a plan can lead to, is hatched as the inventory's
      // extras are. It shows its first item, and how many it holds in the corner opposite the
      // item's own count.
      L.when(stacks.size > 1)(
        List(
          L.cls(Styles.conflict),
          L.span(L.cls(Styles.conflictCount), s"×${stacks.size}")
        )
      ),
      stacks.headOption.map(stack =>
        StackElement(
          stack,
          tooltip,
          tooltipConfig = FloatingConfig.basicTooltip(Placement.bottom, offset = 6),
          hideTooltip = itemCards.isOpenOn,
          customTooltip = Option.when(stacks.size > 1)(conflictTooltip(slot, stacks))
        ).amend(
          L.cls(Styles.contents),
          itemCards.trigger(Holding(stack.item, stack.noted, slot)),
          itemDrag.source(Holding(stack.item, stack.noted, slot))
        )
      )
    )

  private object Backgrounds {
    @js.native @JSImport("/images/equipment/filled-slot.png", JSImport.Default)
    val filled: String = js.native
    @js.native @JSImport("/images/equipment/ammo-slot.png", JSImport.Default)
    val ammo: String = js.native
    @js.native @JSImport("/images/equipment/body-slot.png", JSImport.Default)
    val body: String = js.native
    @js.native @JSImport("/images/equipment/cape-slot.png", JSImport.Default)
    val cape: String = js.native
    @js.native @JSImport("/images/equipment/feet-slot.png", JSImport.Default)
    val feet: String = js.native
    @js.native @JSImport("/images/equipment/hands-slot.png", JSImport.Default)
    val hands: String = js.native
    @js.native @JSImport("/images/equipment/head-slot.png", JSImport.Default)
    val head: String = js.native
    @js.native @JSImport("/images/equipment/legs-slot.png", JSImport.Default)
    val legs: String = js.native
    @js.native @JSImport("/images/equipment/neck-slot.png", JSImport.Default)
    val neck: String = js.native
    @js.native @JSImport("/images/equipment/ring-slot.png", JSImport.Default)
    val ring: String = js.native
    @js.native @JSImport("/images/equipment/shield-slot.png", JSImport.Default)
    val shield: String = js.native
    @js.native @JSImport("/images/equipment/weapon-slot.png", JSImport.Default)
    val weapon: String = js.native
  }

  private def conflictTooltip(slot: EquipmentSlot, stacks: List[ItemStack]): L.Div =
    L.div(
      L.cls(Styles.conflictTooltip),
      L.p(slot.name),
      L.p(L.cls(Styles.conflictNote), s"Holds ${stacks.size} items, but only has room for 1"),
      L.ul(
        L.cls(Styles.conflictItems),
        stacks.map(stack =>
          L.li(
            StackIcon(stack),
            if (stack.quantity > 1) s"${stack.quantity.withCommas} × ${stack.item.name}" else stack.item.name
          )
        )
      )
    )

  @js.native @JSImport("/styles/planning/player/item/equipment/equipmentSlotElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val slot: String = js.native
    val contents: String = js.native
    val background: String = js.native
    val conflict: String = js.native
    val conflictCount: String = js.native
    val conflictTooltip: String = js.native
    val conflictNote: String = js.native
    val conflictItems: String = js.native
  }

  private def toBackground(slot: EquipmentSlot, isEmpty: Boolean): String =
    if (!isEmpty)
      Backgrounds.filled
    else
      slot match {
        case EquipmentSlot.Head => Backgrounds.head
        case EquipmentSlot.Cape => Backgrounds.cape
        case EquipmentSlot.Neck => Backgrounds.neck
        case EquipmentSlot.Ammo => Backgrounds.ammo
        case EquipmentSlot.Weapon => Backgrounds.weapon
        case EquipmentSlot.Shield => Backgrounds.shield
        case EquipmentSlot.Body => Backgrounds.body
        case EquipmentSlot.Legs => Backgrounds.legs
        case EquipmentSlot.Hands => Backgrounds.hands
        case EquipmentSlot.Feet => Backgrounds.feet
        case EquipmentSlot.Ring => Backgrounds.ring
      }
}
