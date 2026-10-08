package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.{EquipmentType, Item}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.plan.ItemQuantity
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot

/** Works out the move that equips an item, and what equipping it displaces. The slot comes from
  * the item data. A move into an item's own slot takes off whatever is in the way where it applies,
  * as the game does, so that a plan stays right when earlier steps change what's worn. Displaced
  * items go back where the item came from, the bank or else the inventory: the slot's current item,
  * the shield for a two-handed weapon, or a two-handed weapon for a shield.
  */
object EquipPlan {
  /** @param source where the item is taken from, unnoted
    * @param quantity how many of a stackable item to equip, such as the bank's withdraw quantity.
    *                 Anything else is equipped one at a time.
    * @return the move, or None if the item can't be equipped or isn't held
    */
  def apply(
    item: Item,
    source: Depository.Kind,
    player: Player,
    items: Item.ID => Item,
    quantity: ItemQuantity = ItemQuantity.Max
  ): Option[MoveItem] =
    for {
      equipmentType <- item.equipmentType
      held = player.get(source).count(item.id, noted = false)
      if held > 0
    } yield {
      val slot = EquipmentSlot.from(equipmentType)
      val equipped = quantity match {
        case _ if !item.stackable => ItemQuantity.Exact(1)
        case ItemQuantity.Exact(n) => ItemQuantity.Exact(math.min(n, held))
        case ItemQuantity.Max => ItemQuantity.Max
      }
      MoveItem(item.id, equipped, source, notedInSource = false, slot, noteInTarget = false)
    }

  /** The moves that clear the way for a move into an item's own slot, applied just before it. A
    * move that can't equip the item, such as into another slot or from notes, displaces nothing:
    * the plan shows its problem instead. Nor does a move of nothing, as the game wouldn't.
    */
  def displaced(move: MoveItem, player: Player, items: Item.ID => Item): List[MoveItem] = {
    val item = items(move.item)
    item.equipmentType match {
      case Some(equipmentType) if equips(move, items) && ItemEffects.count(move, player, items) > 0 =>
        displaced(item, equipmentType, returnTo(move.source), player, items)
      case _ =>
        List.empty
    }
  }

  /** Whether a move equips its item: into the item's own slot, and not from notes */
  def equips(move: MoveItem, items: Item.ID => Item): Boolean =
    move.target match {
      case slot: EquipmentSlot => !move.notedInSource && items(move.item).equipmentType.map(EquipmentSlot.from).contains(slot)
      case _ => false
    }

  /** The slots a move into a slot may take items out of, whatever the item */
  def slotsCleared(slot: EquipmentSlot): Set[EquipmentSlot] =
    slot match {
      case EquipmentSlot.Weapon | EquipmentSlot.Shield => Set(EquipmentSlot.Weapon, EquipmentSlot.Shield)
      case other => Set(other)
    }

  /** Where items displaced by equipping from a place go: the bank for the bank, and otherwise the
    * inventory */
  def returnTo(source: Depository.Kind): Depository.Kind =
    if (source == Depository.Kind.Bank) Depository.Kind.Bank else Depository.Kind.Inventory

  /** Moves the equipped item back to the inventory, or to the bank */
  def unequip(item: Item, slot: EquipmentSlot, target: Depository.Kind): MoveItem =
    MoveItem(item.id, unequipped(item), slot, notedInSource = false, target, noteInTarget = false)

  /** One of an unstackable item, since a slot holds one. A stackable item comes off as a whole
    * stack, whatever is equipped where the step applies. */
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
        case ((current, _), _)
          // Equipping more of a stackable item that's already equipped adds to the equipped stack
          if !(current == item.id && item.stackable) &&
            items(current).equipmentType.exists(conflictingTypes.contains) =>
          MoveItem(current, unequipped(items(current)), slot, notedInSource = false, returnTo, noteInTarget = false)
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
