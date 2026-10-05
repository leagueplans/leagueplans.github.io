package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.{EquipmentType, Item}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, DepositAll, DepositSource, MoveItem}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** What can be done to a stack of items from its card or by dragging it. Each action is the
  * effects to add to the focused step, with sentences to describe it.
  */
object ItemActions {
  /** A stack of an item, and where it's held */
  final case class Holding(item: Item, noted: Boolean, place: Depository.Kind)

  /** @param report says what was done, such as "Banked 25 × Logs"
    * @param preview says what will be done, such as "Bank 25 × Logs"
    */
  final case class Action(effects: List[Effect], report: String, preview: String, detail: Option[String] = None)

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

  /** An exact amount becomes a signed change, and Max becomes the change given for it */
  private def changeOf(quantity: ItemQuantity, all: ItemChange, signed: Int => Int): ItemChange =
    quantity match {
      case ItemQuantity.Exact(n) => ItemChange.By(signed(n))
      case ItemQuantity.Max => all
    }

  def bank(holding: Holding, quantity: ItemQuantity): Action =
    Action(
      List(MoveItem(holding.item.id, quantity, holding.place, holding.noted, Kind.Bank, noteInTarget = false)),
      s"Banked ${describe(holding.item, quantity)}",
      s"Bank ${describe(holding.item, quantity)}"
    )

  def withdraw(holding: Holding, quantity: ItemQuantity, noted: Boolean): Action = {
    val what = s"${describe(holding.item, quantity)}${if (noted) " as notes" else ""}"
    Action(
      List(MoveItem(holding.item.id, quantity, Kind.Bank, notedInSource = false, Kind.Inventory, noteInTarget = noted)),
      s"Withdrew $what",
      s"Withdraw $what"
    )
  }

  def remove(holding: Holding, quantity: ItemQuantity): Action =
    Action(
      List(AddItem(holding.item.id, changeOf(quantity, ItemChange.Empty, -_), holding.place, holding.noted)),
      s"Removed ${describe(holding.item, quantity)}",
      s"Remove ${describe(holding.item, quantity)}"
    )

  /** New copies of a stack's item always go to the inventory, noted if the stack is */
  def addMore(holding: Holding, quantity: ItemQuantity): Action =
    add(holding.item, quantity, Kind.Inventory, holding.noted)

  /** With Max, adds as many as fit in the place */
  def add(item: Item, quantity: ItemQuantity, target: Depository.Kind, noted: Boolean): Action = {
    val effect = AddItem(item.id, changeOf(quantity, ItemChange.Fill, identity), target, noted && item.noteable)
    val place = target.name.toLowerCase
    quantity match {
      case ItemQuantity.Max =>
        Action(List(effect), s"Added ${item.name} until the $place was full", s"Add ${item.name} until the $place is full")
      case ItemQuantity.Exact(_) =>
        val what = s"${describe(item, quantity)} to the $place"
        Action(List(effect), s"Added $what", s"Add $what")
    }
  }

  /** @param quantity how many of a stackable item to wear, such as the bank's withdraw quantity */
  def wear(
    holding: Holding,
    player: Player,
    items: Item.ID => Item,
    quantity: ItemQuantity = ItemQuantity.Max
  ): Option[Action] =
    EquipPlan(holding.item, holding.place, player, items, quantity).map { moves =>
      val displaced = moves.init.map(move => items(move.item).name).mkString(" and ")
      val verb = wearLabel(holding.item)
      Action(
        moves,
        s"${if (verb == "Wield") "Wielded" else "Wore"} ${holding.item.name}",
        s"$verb ${holding.item.name}${if (displaced.isEmpty) "" else s", taking off $displaced"}",
        Option.when(displaced.nonEmpty)(s"Took off $displaced")
      )
    }

  /** Takes off the worn item, into the inventory or the bank */
  def unequip(holding: Holding, target: Depository.Kind): Option[Action] =
    holding.place match {
      case slot: EquipmentSlot =>
        val what = holding.item.name
        Some(Action(
          List(EquipPlan.unequip(holding.item, slot, target)),
          if (target == Kind.Bank) s"Banked $what" else s"Took off $what",
          if (target == Kind.Bank) s"Bank $what" else s"Take off $what"
        ))
      case _ =>
        None
    }

  /** Banks everything in the inventory when the step applies, leaving items that can't be banked,
    * or None if there's nothing to bank here */
  def depositInventory(player: Player, items: Item.ID => Item): Option[Action] =
    deposit(DepositSource.Inventory, player, items, "the inventory")

  /** Banks every worn item that can be banked when the step applies, or None if there's nothing
    * to bank here */
  def depositWorn(player: Player, items: Item.ID => Item): Option[Action] =
    deposit(DepositSource.Equipment, player, items, "worn items")

  private def deposit(source: DepositSource, player: Player, items: Item.ID => Item, what: String): Option[Action] = {
    val stacks = ItemEffects.deposits(source, player, items).size
    Option.when(stacks > 0)(
      Action(
        List(DepositAll(source)),
        s"Deposited $what",
        s"Deposit $what",
        Some(if (stacks == 1) "1 stack at this step" else s"$stacks stacks at this step")
      )
    )
  }

  def describe(item: Item, quantity: ItemQuantity): String =
    quantity match {
      case ItemQuantity.Exact(1) => item.name
      case ItemQuantity.Exact(n) => s"${n.withCommas} × ${item.name}"
      case ItemQuantity.Max => s"all ${item.name}"
    }
}
