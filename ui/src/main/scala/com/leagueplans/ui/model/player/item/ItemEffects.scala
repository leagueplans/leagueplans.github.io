package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.{AddItem, BuyBankSpace, DepositAll, DepositSource, MoveItem, SetBankPin}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot

/** What the item effects do to a player, including those that change the bank's space. Kept apart
  * from the rest of the effects so that the step details can work out what a `Max` comes to
  * without the whole effect resolver.
  */
object ItemEffects {
  /** The effects that change what's held, or where it can go */
  type Change = AddItem | MoveItem | DepositAll | SetBankPin.type | BuyBankSpace

  /** How many of an item an effect adds, moves or removes, when applied to this player. A removal
    * counts the items it takes away. */
  def count(effect: AddItem | MoveItem, player: Player, items: Item.ID => Item): Int =
    effect match {
      case AddItem(_, ItemChange.By(n), _, _) => math.abs(n)
      // Fill: as many as fit. A place with no limit for the item can't be filled.
      case AddItem(item, ItemChange.Fill, target, note) => room(items(item), note, target, player, items).getOrElse(0)
      case AddItem(item, ItemChange.Empty, target, note) => player.get(target).count(item, note)
      case MoveItem(_, ItemQuantity.Exact(n), _, _, _, _) => n
      // Withdrawing Max takes as many as fit in the inventory, as the game does. Every other move
      // takes everything held, even into a full place, where the plan shows the problem.
      case MoveItem(item, ItemQuantity.Max, Kind.Bank, notedInSource, Kind.Inventory, noteInTarget) =>
        val available = player.get(Kind.Bank).count(item, notedInSource)
        room(items(item), noteInTarget, Kind.Inventory, player, items).fold(available)(math.min(available, _))
      case MoveItem(item, ItemQuantity.Max, source, notedInSource, _, _) =>
        player.get(source).count(item, notedInSource)
    }

  def apply(player: Player, effect: Change, items: Item.ID => Item): Player =
    effect match {
      case add: AddItem =>
        val n = count(add, player, items)
        change(player, add.target, add.item, add.note, if (add.change.removes) -n else n)
      case move: MoveItem =>
        val n = count(move, player, items)
        change(
          change(player, move.target, move.item, move.noteInTarget, n),
          move.source, move.item, move.notedInSource, -n
        )
      case DepositAll(source) =>
        deposits(source, player, items).foldLeft(player)(apply(_, _, items))
      case SetBankPin =>
        player.copy(bankSpace = player.bankSpace.copy(unlocks = player.bankSpace.unlocks + BankSpace.Unlock.Pin))
      // The slots are gained even if the coins can't be found, or the blocks before it weren't
      // bought, so that the rest of the plan isn't thrown off by one mistake, which the plan
      // shows as a problem. A block that's already bought can't be bought again.
      case BuyBankSpace(block) if block <= player.bankSpace.blocksBought =>
        player
      case BuyBankSpace(block) =>
        BankSpace.price(block).fold(player) { price =>
          val paid = coinsFor(block, player).fold(player)(place =>
            change(player, place, BankSpace.coins, noted = false, -price)
          )
          paid.copy(bankSpace = paid.bankSpace.copy(blocksBought = paid.bankSpace.blocksBought + 1))
        }
    }

  /** Where the coins for a block of bank space come from: the inventory if it holds enough, and
    * otherwise the bank if it does. The banker won't take some from each. */
  def coinsFor(block: Int, player: Player): Option[Kind] =
    BankSpace.price(block).flatMap(price =>
      List(Kind.Inventory, Kind.Bank).find(player.get(_).count(BankSpace.coins, noted = false) >= price)
    )

  /** The moves a deposit makes: every bankable stack in the place, into the bank, unnoted */
  def deposits(source: DepositSource, player: Player, items: Item.ID => Item): List[MoveItem] =
    for {
      kind <- source.places
      ((id, noted), quantity) <- player.get(kind).contents.toList.sortBy { case ((id, noted), _) => (items(id).name, noted) }
      if items(id).bankable != Item.Bankable.No
    } yield MoveItem(id, ItemQuantity.Exact(quantity), kind, noted, Kind.Bank, noteInTarget = false)

  /** Whether a place can be filled with the item: only where each item takes a slot of its own */
  def canFill(item: Item, noted: Boolean, target: Depository.Kind): Boolean =
    target == Kind.Inventory && !item.stackable && !noted

  /** How many more of an item a place has room for, or None if there's no limit, as for a stack
    * that's already held. Each unstacked item in the inventory takes a slot of its own. */
  def room(item: Item, noted: Boolean, target: Depository.Kind, player: Player, items: Item.ID => Item): Option[Int] = {
    val depository = player.get(target)
    val alreadyHeld = depository.contents.contains((item.id, noted))
    target match {
      // As in the game, an item takes one slot for its whole stack, or one for each item if it
      // doesn't stack in the bank
      case Kind.Bank =>
        def stacksInBank(item: Item): Boolean = item.bankable != Item.Bankable.Yes(stacks = false)
        val used = depository.contents.toList.map { case ((id, _), n) => if (stacksInBank(items(id))) 1 else n }.sum
        val free = math.max(player.capacity(Kind.Bank) - used, 0)
        if (stacksInBank(item)) Option.when(!alreadyHeld && free == 0)(0)
        else Some(free)
      case Kind.Inventory =>
        val used = depository.contents.toList.map { case ((id, isNoted), n) =>
          if (items(id).stackable || isNoted) 1 else n
        }.sum
        val free = math.max(Kind.Inventory.capacity - used, 0)
        if (item.stackable || noted) Option.when(!alreadyHeld && free == 0)(0)
        else Some(free)
      case _: EquipmentSlot =>
        if (depository.contents.isEmpty) Option.when(!item.stackable)(1)
        else if (item.stackable && alreadyHeld) None
        else Some(0)
    }
  }

  private def change(player: Player, kind: Depository.Kind, item: Item.ID, noted: Boolean, by: Int): Player = {
    val depository = player.get(kind)
    val key = (item, noted)
    val updated = depository.count(item, noted) + by
    val contents = if (updated <= 0) depository.contents - key else depository.contents + (key -> updated)
    player.copy(depositories = player.depositories + (kind -> depository.copy(contents = contents)))
  }
}
