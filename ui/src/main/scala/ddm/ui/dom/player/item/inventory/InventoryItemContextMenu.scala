package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.EquipmentType
import ddm.common.model.Item.Bankable
import ddm.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import ddm.ui.dom.player.item.MoveItemForm
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.{AddItem, MoveItem}
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.model.player.item.{Depository, Stack}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.utils.laminar.LaminarOps.handled

object InventoryItemContextMenu {
  private val inventory = Depository.Kind.Inventory

  def apply(
    stack: Stack,
    cache: Cache,
    stackSize: Int,
    playerSignal: Signal[Player],
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalController: Modal.Controller
  ): L.Div = {
    val heldQuantitySignal = playerSignal.map(
      _.get(inventory).contents.getOrElse((stack.item.id, stack.noted), 0)
    )

    L.div(
      L.child <-- heldQuantitySignal.map(bankButton(stack, _, effectObserver, menuCloser, modalController)),
      L.child <-- playerSignal.map(player =>
        equipButton(stack, cache, stackSize, player, effectObserver, menuCloser)
      ),
      L.child <-- heldQuantitySignal.map(removeButton(stack, _, effectObserver, menuCloser, modalController)),
    )
  }

  private def bankButton(
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalController: Modal.Controller
  ): L.Node =
    stack.item.bankable match {
      case Bankable.No => L.emptyNode
      case _: Bankable.Yes =>
        val observer =
          if (heldQuantity > 1)
            toBankItemFormOpener(stack, heldQuantity, effectObserver, modalController)
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
    modalController: Modal.Controller
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(
      stack,
      heldQuantity,
      inventory,
      Depository.Kind.Bank,
      noteInTarget = false
    )
    FormOpener(
      modalController,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

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
    modalController: Modal.Controller
  ): L.Button = {
    val observer =
      if (heldQuantity > 1)
        toRemoveItemFormOpener(stack, heldQuantity, effectObserver, modalController)
      else
        effectObserver.contramap[Unit](_ => AddItem(stack.item.id, -heldQuantity, inventory, stack.noted))

    button("Remove", observer, menuCloser)
  }

  private def toRemoveItemFormOpener(
    stack: Stack,
    heldQuantity: Int,
    effectObserver: Observer[AddItem],
    modalController: Modal.Controller
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = RemoveItemForm(stack, heldQuantity, inventory)
    FormOpener(
      modalController,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

  private def button(
    text: String,
    clickObserver: Observer[Unit],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      Observer.combine(clickObserver, menuCloser)
    )(_.handled).amend(text)
}
