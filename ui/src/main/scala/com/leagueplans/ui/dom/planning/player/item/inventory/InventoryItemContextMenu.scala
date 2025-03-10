package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.EquipmentType
import com.leagueplans.common.model.Item.Bankable
import com.leagueplans.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.player.item.MoveItemForm
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{AddItem, MoveItem}
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.{Depository, Stack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object InventoryItemContextMenu {
  private val inventory = Depository.Kind.Inventory

  def apply(
    stack: Stack,
    cache: Cache,
    stackSize: Int,
    playerSignal: Signal[Player],
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Div = {
    val heldQuantitySignal = playerSignal.map(
      _.get(inventory).contents.getOrElse((stack.item.id, stack.noted), 0)
    )

    L.div(
      L.child <-- heldQuantitySignal.map(bankButton(stack, _, effectObserver, menuCloser, modal)),
      L.child <-- playerSignal.map(player =>
        equipButton(stack, cache, stackSize, player, effectObserver, menuCloser)
      ),
      L.child <-- heldQuantitySignal.map(removeButton(stack, _, effectObserver, menuCloser, modal)),
    )
  }

  private def bankButton(
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Node =
    stack.item.bankable match {
      case Bankable.No => L.emptyNode
      case _: Bankable.Yes =>
        val observer =
          if (heldQuantity > 1)
            toBankItemFormOpener(stack, heldQuantity, effectObserver, modal).toObserver
          else
            effectObserver.contramap[Unit](_ =>
              MoveItem(
                stack.item.id,
                heldQuantity,
                inventory,
                stack.noted,
                Depository.Kind.Bank,
                noteInTarget = false
              )
            )

        button("Bank", observer, menuCloser)
    }

  private def toBankItemFormOpener(
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    modal: Modal
  ): FormOpener =
    FormOpener(
      modal,
      MoveItemForm(
        stack,
        heldQuantity,
        inventory,
        Depository.Kind.Bank,
        noteInTarget = false
      ),
      effectObserver.contracollect[Option[MoveItem]] { case Some(effect) => effect }
    )

  private def equipButton(
    stack: Stack,
    cache: Cache,
    stackSize: Int,
    player: Player,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Node =
    (stack.noted, stack.item.equipmentType) match {
      case (false, Some(tpe)) =>
        val equipEffect: MoveItem = MoveItem(
          stack.item.id,
          stackSize,
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
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[AddItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Button = {
    val observer =
      if (heldQuantity > 1)
        toRemoveItemFormOpener(stack, heldQuantity, effectObserver, modal).toObserver
      else
        effectObserver.contramap[Unit](_ => AddItem(stack.item.id, -heldQuantity, inventory, stack.noted))

    button("Remove", observer, menuCloser)
  }

  private def toRemoveItemFormOpener(
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[AddItem],
    modal: Modal
  ): FormOpener =
    FormOpener(
      modal,
      RemoveItemForm(stack, heldQuantity, inventory),
      effectObserver.contracollect[Option[AddItem]] { case Some(effect) => effect }
    )

  private def button(
    text: String,
    clickObserver: Observer[Unit],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(_.handled --> Observer.combine(clickObserver, menuCloser)).amend(text)
}
