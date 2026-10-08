package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, BuyBankSpace, DepositAll, DepositSource, MoveItem, SetBankPin}
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

  /** Items can be equipped from the inventory, or straight from the bank */
  def canEquip(holding: Holding): Boolean =
    (holding.place == Kind.Inventory || holding.place == Kind.Bank) &&
      !holding.noted &&
      holding.item.equipmentType.nonEmpty

  /** Why adding Max isn't possible: only items that each take a slot can be added until the
    * inventory's full */
  val fillReason: String = "Adding until full only works for items that take an inventory slot each"

  /** A button on a stack's card */
  enum CardButton {
    /** Acts on the whole stack */
    case Whole(label: String, act: Player => Option[Action])

    /** Takes the amount entered on the card
      *
      * @param secondary whether it's shown less prominently, as removing and adding are
      * @param maxLabel the label when the amount is Max, if not "<label> all"
      * @param unavailable why it can't take an amount, if there's one it can't take
      */
    case WithAmount(
      label: String,
      secondary: Boolean,
      maxLabel: Option[String],
      unavailable: ItemQuantity => Option[String],
      act: (ItemQuantity, Player) => Option[Action]
    )
  }

  /** The buttons on a stack's card. Only the inventory's cards add or remove items: on the bank's
    * and the equipment's cards, those would read as moves, which they aren't.
    *
    * Amounts above what's held, or more than there's room for, are allowed: the plan shows the
    * problem, and it can help while other steps are still being changed.
    */
  def cardButtons(holding: Holding, items: Item.ID => Item): List[CardButton] = {
    def amount(label: String, secondary: Boolean = false, maxLabel: Option[String] = None)(
      act: ItemQuantity => Action
    ): CardButton =
      CardButton.WithAmount(label, secondary, maxLabel, _ => None, (n, _) => Some(act(n)))

    val equipWhole = CardButton.Whole("Equip", equip(holding, _, items))
    holding.place match {
      case Kind.Inventory =>
        Option.when(canEquip(holding))(equipWhole).toList ++
          Option.when(canBank(holding))(amount("Bank")(bank(holding, _))).toList ++
          List(
            amount("Remove", secondary = true)(remove(holding, _)),
            CardButton.WithAmount(
              "Add",
              secondary = true,
              Some("Add until full"),
              {
                case ItemQuantity.Max if !ItemEffects.canFill(holding.item, holding.noted, Kind.Inventory) => Some(fillReason)
                case _ => None
              },
              (n, _) => Some(addMore(holding, n))
            )
          )

      case Kind.Bank =>
        // Only one of an unstackable item can be equipped, so there's no amount to take
        Option.when(canEquip(holding) && !holding.item.stackable)(equipWhole).toList ++
          List(
            // Max withdraws as many as fit in the inventory, which is all of them if they stack
            Some(amount("Withdraw", maxLabel = Option.when(!holding.item.stackable)("Withdraw until full"))(
              withdraw(holding, _, noted = false)
            )),
            Option.when(canWithdrawNoted(holding))(
              amount("Withdraw noted", maxLabel = Some("Withdraw all noted"))(withdraw(holding, _, noted = true))
            ),
            Option.when(canEquip(holding) && holding.item.stackable)(
              CardButton.WithAmount("Equip", secondary = false, None, _ => None, (n, player) => equip(holding, player, items, n))
            )
          ).flatten

      case _: EquipmentSlot =>
        List(
          Some(CardButton.Whole("Unequip", _ => unequip(holding, Kind.Inventory))),
          Option.when(holding.item.bankable != Item.Bankable.No)(CardButton.Whole("Bank", _ => unequip(holding, Kind.Bank))),
          // A worn stack, such as arrows or chinchompas, is used up as it's fired or thrown
          Option.when(holding.item.stackable)(amount("Remove", secondary = true)(remove(holding, _)))
        ).flatten
    }
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
    quantity match {
      case ItemQuantity.Max =>
        Action(List(effect), s"Added ${item.name} until the ${target.name.toLowerCase} was full", s"Add ${item.name} until the ${target.name.toLowerCase} is full")
      case ItemQuantity.Exact(_) =>
        val what = s"${describe(item, quantity)} to the ${target.name.toLowerCase}"
        Action(List(effect), s"Added $what", s"Add $what")
    }
  }

  /** @param quantity how many of a stackable item to equip, such as the bank's withdraw quantity */
  def equip(
    holding: Holding,
    player: Player,
    items: Item.ID => Item,
    quantity: ItemQuantity = ItemQuantity.Max
  ): Option[Action] =
    EquipPlan(holding.item, holding.place, player, items, quantity).map { moves =>
      val displaced = moves.init.map(move => items(move.item).name).mkString(" and ")
      Action(
        moves,
        s"Equipped ${holding.item.name}",
        s"Equip ${holding.item.name}${if (displaced.isEmpty) "" else s", unequipping $displaced"}",
        Option.when(displaced.nonEmpty)(s"Unequipped $displaced")
      )
    }

  /** Unequips the whole equipped stack, into the inventory or the bank */
  def unequip(holding: Holding, target: Depository.Kind): Option[Action] =
    holding.place match {
      case slot: EquipmentSlot =>
        val what = holding.item.name
        Some(Action(
          List(EquipPlan.unequip(holding.item, slot, target)),
          if (target == Kind.Bank) s"Banked $what" else s"Unequipped $what",
          if (target == Kind.Bank) s"Bank $what" else s"Unequip $what"
        ))
      case _ =>
        None
    }

  /** Sets a bank PIN, or None if one's already set */
  def setBankPin(player: Player): Option[Action] =
    Option.when(!player.bankSpace.unlocks.contains(BankSpace.Unlock.Pin))(
      Action(List(SetBankPin), "Set a bank PIN", "Set a bank PIN", Some(s"+${BankSpace.unlockSlots} bank slots"))
    )

  /** Buys the next block of bank space, or None once they're all bought. Its detail says where the
    * coins come from, or that there aren't enough, which the plan also shows as a problem. */
  def buyBankSpace(player: Player): Option[Action] = {
    val block = player.bankSpace.blocksBought + 1
    BankSpace.price(block).map { price =>
      val paidFrom = ItemEffects.coinsFor(block, player) match {
        case Some(place) => s"paid from the ${place.name.toLowerCase}"
        case None => "but neither the inventory nor the bank holds enough coins"
      }
      Action(
        List(BuyBankSpace(block)),
        s"Bought bank space block $block",
        s"Buy block $block for ${price.withCommas} coins",
        Some(s"+${BankSpace.blockSlots} bank slots, $paidFrom")
      )
    }
  }

  /** Banks everything in the inventory when the step applies, leaving items that can't be banked,
    * or None if there's nothing to bank here */
  def depositInventory(player: Player, items: Item.ID => Item): Option[Action] =
    deposit(DepositSource.Inventory, player, items, "the inventory")

  /** Banks every equipped item that can be banked when the step applies, or None if there's nothing
    * to bank here */
  def depositEquipment(player: Player, items: Item.ID => Item): Option[Action] =
    deposit(DepositSource.Equipment, player, items, "equipment")

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
