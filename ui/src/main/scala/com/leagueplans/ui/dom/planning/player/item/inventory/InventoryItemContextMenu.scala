package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.EquipmentType
import com.leagueplans.common.model.Item.Bankable
import com.leagueplans.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.player.item.MoveItemForm
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{AddItem, MoveItem}
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object InventoryItemContextMenu {
  private val inventory = Depository.Kind.Inventory

  def apply(
    stack: ItemStack,
    cache: Cache,
    playerSignal: Signal[Player],
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Div = {
    val allStacksSignal = playerSignal.map(player =>
      stack.copy(quantity =
        player.get(inventory).contents.getOrElse((stack.item.id, stack.noted), 0)
      )
    )

    L.div(
      L.child <-- allStacksSignal.map(bankButton(_, effectObserver, menuCloser, modal)),
      L.child <-- playerSignal.map(equipButton(stack, cache, _, effectObserver, menuCloser)),
      L.child <-- allStacksSignal.map(removeButton(_, effectObserver, menuCloser, modal)),
    )
  }

  private def bankButton(
    stack: ItemStack,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Node =
    stack.item.bankable match {
      case Bankable.No => L.emptyNode
      case _: Bankable.Yes =>
        val observer =
          if (stack.quantity > 1)
            toBankItemFormOpener(stack, effectObserver, modal).toObserver
          else
            effectObserver.contramap[Unit](_ =>
              MoveItem(
                stack.item.id,
                stack.quantity,
                inventory,
                stack.noted,
                Depository.Kind.Bank,
                noteInTarget = false
              )
            )

        button("Bank", observer, menuCloser)
    }

  private def toBankItemFormOpener(
    stack: ItemStack,
    effectObserver: Observer[MoveItem],
    modal: Modal
  ): FormOpener =
    FormOpener(
      modal,
      MoveItemForm(stack, inventory, Depository.Kind.Bank, noteInTarget = false),
      effectObserver.contracollect[Option[MoveItem]] { case Some(effect) => effect }
    )

  private def equipButton(
    stack: ItemStack,
    cache: Cache,
    player: Player,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Node =
    (stack.noted, stack.item.equipmentType) match {
      case (false, Some(tpe)) =>
        val equipEffect: MoveItem = MoveItem(
          stack.item.id,
          stack.quantity,
          inventory,
          notedInSource = false,
          EquipmentSlot.from(tpe),
          noteInTarget = false
        )

        val unequipEffects = toConflicts(tpe).flatMap((slot, conflictTypes) =>
          player.get(slot).contents.flatMap { case ((currentlyEquipped, _), equippedStackSize) =>
            val sameStackableItem = stack.item.id == currentlyEquipped && stack.item.stackable
            val conflictedType = cache.items(currentlyEquipped).equipmentType.exists(conflictTypes.contains)
            Option.when[MoveItem](!sameStackableItem && conflictedType)(
              MoveItem(
                currentlyEquipped,
                equippedStackSize,
                slot,
                notedInSource = false,
                inventory,
                noteInTarget = false
              )
            )
          }
        )

        val observer = Observer[Unit](_ =>
          (unequipEffects.toList :+ equipEffect).foreach(effectObserver.onNext)
        )
        button("Equip", observer, menuCloser)

      case _ =>
        L.emptyNode
    }

  private def toConflicts(equipmentType: EquipmentType): Set[(EquipmentSlot, Set[EquipmentType])] =
    equipmentType match {
      case EquipmentType.Weapon =>
        Set(EquipmentSlot.Weapon -> Set(EquipmentType.Weapon, EquipmentType.TwoHanded))
      case EquipmentType.Shield =>
        Set(
          EquipmentSlot.Weapon -> Set(EquipmentType.TwoHanded),
          EquipmentSlot.Shield -> Set(EquipmentType.Shield)
        )
      case EquipmentType.TwoHanded =>
        Set(
          EquipmentSlot.Weapon -> Set(EquipmentType.Weapon, EquipmentType.TwoHanded),
          EquipmentSlot.Shield -> Set(EquipmentType.Shield)
        )
      case other =>
        Set(EquipmentSlot.from(other) -> Set(other))
    }

  private def removeButton(
    stack: ItemStack,
    effectObserver: Observer[AddItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Button = {
    val observer =
      if (stack.quantity > 1)
        toRemoveItemFormOpener(stack, effectObserver, modal).toObserver
      else
        effectObserver.contramap[Unit](_ => AddItem(stack.item.id, -stack.quantity, inventory, stack.noted))

    button("Remove", observer, menuCloser)
  }

  private def toRemoveItemFormOpener(
    stack: ItemStack,
    effectObserver: Observer[AddItem],
    modal: Modal
  ): FormOpener =
    FormOpener(
      modal,
      RemoveItemForm(stack, inventory),
      effectObserver.contracollect[Option[AddItem]] { case Some(effect) => effect }
    )

  private def button(
    text: String,
    clickObserver: Observer[Unit],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(_.handled --> Observer.combine(clickObserver, menuCloser)).amend(text)
}
