package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.ItemRoute.Place

object ItemRoute {
  /** Somewhere an item can be, including whether it's noted there */
  final case class Place(kind: Kind, noted: Boolean) {
    def label: String =
      if (noted) s"${kind.name} (noted)" else kind.name
  }

  private val inventory = Place(Kind.Inventory, noted = false)
  private val notedInventory = Place(Kind.Inventory, noted = true)
  private val bank = Place(Kind.Bank, noted = false)

  /** The ways an item can be moved in the game. Notes only come from withdrawing from the bank,
    * and only go back into it, where they're unnoted. Only unnoted items are worn. */
  def allFor(item: Item): List[ItemRoute] = {
    val banks = item.bankable != Item.Bankable.No
    val slot = item.equipmentType.map(tpe => Place(Kind.EquipmentSlot.from(tpe), noted = false))

    List(
      Option.when(banks)(ItemRoute(bank, inventory)),
      Option.when(banks && item.noteable)(ItemRoute(bank, notedInventory)),
      Option.when(banks)(ItemRoute(inventory, bank)),
      Option.when(banks && item.noteable)(ItemRoute(notedInventory, bank)),
      slot.map(ItemRoute(inventory, _)),
      slot.map(ItemRoute(_, inventory)),
      slot.filter(_ => banks).map(ItemRoute(_, bank)),
      slot.filter(_ => banks).map(ItemRoute(bank, _))
    ).flatten
  }

  def of(move: Effect.MoveItem): ItemRoute =
    ItemRoute(Place(move.source, move.notedInSource), Place(move.target, move.noteInTarget))

  def isPossible(move: Effect.MoveItem, item: Item): Boolean =
    allFor(item).contains(of(move))
}

/** Where an item move takes an item from and to */
final case class ItemRoute(from: Place, to: Place) {
  def label: String =
    s"${from.label} → ${to.label}"

  def applyTo(move: Effect.MoveItem): Effect.MoveItem =
    move.copy(source = from.kind, notedInSource = from.noted, target = to.kind, noteInTarget = to.noted)
}
