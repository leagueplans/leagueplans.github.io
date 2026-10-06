package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.ItemQuantity
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.{Action, Holding}

/** Works out what dragging a stack onto a panel does, following the game's bank: a quantity
  * setting (1 / 5 / 10 / X / All) and a Withdraw as Item / Note setting decide how much moves and
  * whether it's noted. Dropping anywhere on the equipment panel equips the item in its own slot.
  * An inventory item that takes a slot to itself, unnoted and unstackable, moves one at a time, as
  * its card does.
  */
object ItemTransfer {
  enum Quantity {
    case One, Five, Ten, All
    /** The last amount entered for X, as in the game */
    case X(amount: Int)
  }

  final case class Settings(quantity: Quantity, withdrawNoted: Boolean)

  object Settings {
    val default: Settings = Settings(Quantity.All, withdrawNoted = false)
  }

  /** The panels a stack can be dropped on */
  enum Target {
    case Inventory, Bank, Equipment
  }

  enum Rejection(val message: String) {
    /** Dropping a stack back where it came from does nothing, and says nothing */
    case SameDepository extends Rejection("")
    case NotBankable extends Rejection("This item can't be banked")
    case NotEquippable extends Rejection("This item can't be equipped")
    case NotedEquip extends Rejection("Noted items can't be equipped")
    case NothingToMove extends Rejection("None of these are held at this step")
  }

  def plan(
    source: Holding,
    target: Target,
    player: Player,
    items: Item.ID => Item,
    settings: Settings
  ): Either[Rejection, Action] =
    transfer(source, target, player, items, settings, oneAtATime = true)

  private def transfer(
    source: Holding,
    target: Target,
    player: Player,
    items: Item.ID => Item,
    settings: Settings,
    oneAtATime: Boolean
  ): Either[Rejection, Action] = {
    val held = ItemActions.held(source, player)
    if (held <= 0)
      Left(Rejection.NothingToMove)
    else
      (source.place, target) match {
        case (Kind.Inventory, Target.Inventory) | (Kind.Bank, Target.Bank) | (_: EquipmentSlot, Target.Equipment) =>
          Left(Rejection.SameDepository)

        case (_, Target.Bank) =>
          if (source.item.bankable == Item.Bankable.No)
            Left(Rejection.NotBankable)
          else
            source.place match {
              case _: EquipmentSlot =>
                toRight(ItemActions.unequip(source, Kind.Bank), Rejection.NothingToMove)
              case Kind.Inventory if oneAtATime && !source.noted && !source.item.stackable =>
                Right(ItemActions.bank(source, ItemQuantity.Exact(1)))
              case _ =>
                Right(ItemActions.bank(source, resolve(settings.quantity, held)))
            }

        // Moves into a full inventory still go through, and the plan shows the problem
        case (Kind.Bank, Target.Inventory) =>
          val noted = settings.withdrawNoted && source.item.noteable
          Right(ItemActions.withdraw(source, resolve(settings.quantity, held), noted))

        case (_: EquipmentSlot, Target.Inventory) =>
          toRight(ItemActions.unequip(source, Kind.Inventory), Rejection.NothingToMove)

        case (_, Target.Equipment) =>
          if (source.item.equipmentType.isEmpty)
            Left(Rejection.NotEquippable)
          else if (source.noted)
            Left(Rejection.NotedEquip)
          else
            // From the bank, stackable items follow the withdraw quantity, as other withdrawals do
            val quantity = if (source.place == Kind.Bank) resolve(settings.quantity, held) else ItemQuantity.Max
            toRight(ItemActions.equip(source, player, items, quantity), Rejection.NothingToMove)
      }
  }

  /** Shift-clicking a stack moves all of it: from the inventory or the equipment to the bank, or
    * from the bank to the inventory (noted if Withdraw as Note is chosen). Equipped items that
    * can't be banked go to the inventory. */
  def quickMove(source: Holding, player: Player, items: Item.ID => Item, settings: Settings): Either[Rejection, Action] = {
    val target = source.place match {
      case Kind.Inventory => Target.Bank
      case _: EquipmentSlot if source.item.bankable != Item.Bankable.No => Target.Bank
      case _ => Target.Inventory
    }
    transfer(source, target, player, items, settings.copy(quantity = Quantity.All), oneAtATime = false)
  }

  /** All becomes Max, so the effect moves whatever is held where it applies. Other quantities take
    * no more than is held now. */
  private def resolve(quantity: Quantity, held: Int): ItemQuantity =
    quantity match {
      case Quantity.One => ItemQuantity.Exact(math.min(1, held))
      case Quantity.Five => ItemQuantity.Exact(math.min(5, held))
      case Quantity.Ten => ItemQuantity.Exact(math.min(10, held))
      case Quantity.X(amount) => ItemQuantity.Exact(math.min(amount, held))
      case Quantity.All => ItemQuantity.Max
    }

  private def toRight(action: Option[Action], rejection: Rejection): Either[Rejection, Action] =
    action.toRight(rejection)
}
