package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.{EquipmentType, Item}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.plan.ItemQuantity
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot

/** Works out the moves that wear an item. The slot comes from the item data. Whatever the item
  * displaces goes back where the item came from, the bank or else the inventory: the slot's
  * current item, the shield for a two-handed weapon, or a two-handed weapon for a shield. Add the
  * moves together, so that equipping is one change to undo.
  */
object EquipPlan {
  /** @param source where the item is taken from, unnoted
    * @param quantity how many of a stackable item to wear, such as the bank's withdraw quantity.
    *                 Anything else is worn one at a time.
    * @return the moves, displaced items first, or None if the item can't be worn or isn't held
    */
  def apply(
    item: Item,
    source: Depository.Kind,
    player: Player,
    items: Item.ID => Item,
    quantity: ItemQuantity = ItemQuantity.Max
  ): Option[List[MoveItem]] =
    for {
      equipmentType <- item.equipmentType
      held = player.get(source).count(item.id, noted = false)
      if held > 0
    } yield {
      val slot = EquipmentSlot.from(equipmentType)
      val worn = quantity match {
        case _ if !item.stackable => ItemQuantity.Exact(1)
        case ItemQuantity.Exact(n) => ItemQuantity.Exact(math.min(n, held))
        case ItemQuantity.Max => ItemQuantity.Max
      }
      val returnTo = if (source == Depository.Kind.Bank) Depository.Kind.Bank else Depository.Kind.Inventory
      displaced(item, equipmentType, returnTo, player, items) :+
        MoveItem(item.id, worn, source, notedInSource = false, slot, noteInTarget = false)
    }

  /** Moves the worn item back to the inventory, or to the bank */
  def unequip(item: Item, slot: EquipmentSlot, target: Depository.Kind): MoveItem =
    MoveItem(item.id, unequipped(item), slot, notedInSource = false, target, noteInTarget = false)

  /** One of an unstackable item, since a slot holds one, so that wearing and taking it off in a
    * step cancel out. A stackable item comes off as a whole stack, whatever is worn where the step
    * applies. */
  private def unequipped(item: Item): ItemQuantity =
    if (item.stackable) ItemQuantity.Max else ItemQuantity.Exact(1)

  private def displaced(
    item: Item,
    equipmentType: EquipmentType,
    returnTo: Depository.Kind,
    player: Player,
    items: Item.ID => Item
  ): List[MoveItem] =
    conflicts(equipmentType).toList.sortBy((slot, _) => slot.ordinal).flatMap((slot, conflictingTypes) =>
      player.get(slot).contents.toList.collect {
        case ((worn, _), _)
          // Wearing more of a stackable item that's already worn adds to the worn stack
          if !(worn == item.id && item.stackable) &&
            items(worn).equipmentType.exists(conflictingTypes.contains) =>
          MoveItem(worn, unequipped(items(worn)), slot, notedInSource = false, returnTo, noteInTarget = false)
      }
    )

  private def conflicts(equipmentType: EquipmentType): Map[EquipmentSlot, Set[EquipmentType]] =
    equipmentType match {
      case EquipmentType.Weapon =>
        Map(EquipmentSlot.Weapon -> Set(EquipmentType.Weapon, EquipmentType.TwoHanded))
      case EquipmentType.Shield =>
        Map(
          EquipmentSlot.Weapon -> Set(EquipmentType.TwoHanded),
          EquipmentSlot.Shield -> Set(EquipmentType.Shield)
        )
      case EquipmentType.TwoHanded =>
        Map(
          EquipmentSlot.Weapon -> Set(EquipmentType.Weapon, EquipmentType.TwoHanded),
          EquipmentSlot.Shield -> Set(EquipmentType.Shield)
        )
      case other =>
        Map(EquipmentSlot.from(other) -> Set(other))
    }
}
