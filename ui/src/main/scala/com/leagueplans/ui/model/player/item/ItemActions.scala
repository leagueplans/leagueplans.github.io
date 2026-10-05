package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.{EquipmentType, Item}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, MoveItem}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** What can be done to a stack of items from its card. Each action is the effects to add to the
  * focused step, with a sentence to report it.
  */
object ItemActions {
  /** A stack of an item, and where it's held */
  final case class Holding(item: Item, noted: Boolean, place: Depository.Kind)

  final case class Action(effects: List[Effect], report: String, detail: Option[String] = None)

  /** How many of the stack are held */
  def held(holding: Holding, player: Player): Int =
    player.get(holding.place).count(holding.item.id, holding.noted)

  def canBank(holding: Holding): Boolean =
    holding.place != Kind.Bank && holding.item.bankable != Item.Bankable.No

  def canWithdrawNoted(holding: Holding): Boolean =
    holding.place == Kind.Bank && holding.item.noteable

  def canWear(holding: Holding): Boolean =
    holding.place == Kind.Inventory && !holding.noted && holding.item.equipmentType.nonEmpty

  /** "Wield" for weapons, "Wear" for anything else */
  def wearLabel(item: Item): String =
    item.equipmentType match {
      case Some(EquipmentType.Weapon | EquipmentType.TwoHanded) => "Wield"
      case _ => "Wear"
    }

  def bank(holding: Holding, quantity: Int): Action =
    Action(
      List(MoveItem(holding.item.id, ItemQuantity.Exact(quantity), holding.place, holding.noted, Kind.Bank, noteInTarget = false)),
      s"Banked ${describe(holding.item, quantity)}"
    )

  def withdraw(holding: Holding, quantity: Int, noted: Boolean): Action =
    Action(
      List(MoveItem(holding.item.id, ItemQuantity.Exact(quantity), Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = noted)),
      s"Withdrew ${describe(holding.item, quantity)}${if (noted) " as notes" else ""}"
    )

  def remove(holding: Holding, quantity: Int): Action =
    Action(
      List(AddItem(holding.item.id, ItemChange.By(-quantity), holding.place, holding.noted)),
      s"Removed ${describe(holding.item, quantity)}"
    )

  /** New copies go to the bank for a banked stack, and to the inventory otherwise */
  def addMore(holding: Holding, quantity: Int): Action = {
    val target = if (holding.place == Kind.Bank) Kind.Bank else Kind.Inventory
    add(holding.item, quantity, target, holding.noted && target == Kind.Inventory)
  }

  def add(item: Item, quantity: Int, target: Depository.Kind, noted: Boolean): Action =
    Action(
      List(AddItem(item.id, ItemChange.By(quantity), target, noted && item.noteable)),
      s"Added ${describe(item, quantity)} to the ${target.name.toLowerCase}"
    )

  def wear(holding: Holding, player: Player, items: Item.ID => Item): Option[Action] =
    EquipPlan(holding.item, holding.place, player, items).map { moves =>
      val displaced = moves.init.map(move => items(move.item).name)
      Action(
        moves,
        s"${if (wearLabel(holding.item) == "Wield") "Wielded" else "Wore"} ${holding.item.name}",
        Option.when(displaced.nonEmpty)(s"Took off ${displaced.mkString(" and ")}")
      )
    }

  /** Takes off the worn item, into the inventory or the bank */
  def unequip(holding: Holding, target: Depository.Kind): Option[Action] =
    holding.place match {
      case slot: EquipmentSlot =>
        Some(Action(
          List(EquipPlan.unequip(holding.item, slot, target)),
          if (target == Kind.Bank) s"Banked ${holding.item.name}" else s"Took off ${holding.item.name}"
        ))
      case _ =>
        None
    }

  def describe(item: Item, quantity: Int): String =
    if (quantity == 1) item.name else s"${quantity.withCommas} × ${item.name}"
}
