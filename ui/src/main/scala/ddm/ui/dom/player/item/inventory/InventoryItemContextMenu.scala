package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item.Bankable
import ddm.common.model.{EquipmentType, Item}
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.dom.player.item.MoveItemForm
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.{GainItem, MoveItem}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.{Button, Div}

object InventoryItemContextMenu {
  private val inventory = Depository.Kind.Inventory

  def apply(
    item: Item,
    itemCache: ItemCache,
    stackSizeSignal: Signal[Int],
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map { maybeEffectObserver =>
        maybeEffectObserver.map(effectObserver =>
          toMenu(item, itemCache, stackSizeSignal, playerSignal, effectObserver, menuCloser, modalBus)
        )
      }
    )

  private def toMenu(
    item: Item,
    itemCache: ItemCache,
    stackSizeSignal: Signal[Int],
    playerSignal: Signal[Player],
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Div] = {
    val heldQuantitySignal = playerSignal.map(_.get(inventory).contents.getOrElse(item.id, 0))
    L.div(
      L.child <-- heldQuantitySignal.map(bankButton(item, _, effectObserver, menuCloser, modalBus)),
      L.child <-- Signal.combine(stackSizeSignal, playerSignal).map { case (stackSize, player) =>
        equipButton(item, itemCache, stackSize, player, effectObserver, menuCloser)
      },
      L.child <-- heldQuantitySignal.map(removeButton(item, _, effectObserver, menuCloser, modalBus)),
    )
  }

  private def bankButton(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): L.Child =
    item.bankable match {
      case Bankable.No => L.emptyNode
      case _: Bankable.Yes =>
        val observer =
          if (heldQuantity > 1)
            toBankItemFormOpener(item, heldQuantity, effectObserver, modalBus)
          else
            effectObserver.contramap[Unit](_ =>
              MoveItem(item.id, heldQuantity, inventory, Depository.Kind.Bank)
            )

        button("Bank", observer, menuCloser)
    }

  private def toBankItemFormOpener(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    modalBus: WriteBus[Option[L.Element]]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(item, heldQuantity, inventory, Depository.Kind.Bank)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

  private def equipButton(
    item: Item,
    itemCache: ItemCache,
    stackSize: Int,
    player: Player,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Child =
    item.equipmentType match {
      case None => L.emptyNode
      case Some(tpe) =>
        val equipEffect = MoveItem(item.id, stackSize, inventory, toSlot(tpe))
        val unequipEffects = toConflicts(tpe).flatMap { case (slot, conflictTypes) =>
          player.get(slot).contents.flatMap { case (currentlyEquipped, equippedStackSize) =>
            val sameStackableItem = item.id == currentlyEquipped && item.stackable
            val conflictedType = itemCache(currentlyEquipped).equipmentType.exists(conflictTypes.contains)
            Option.when(!sameStackableItem && conflictedType)(
              MoveItem(currentlyEquipped, equippedStackSize, slot, inventory)
            )
          }
        }
        val observer = Observer[Unit](_ =>
          (unequipEffects.toList :+ equipEffect).foreach(effectObserver.onNext)
        )

        button("Equip", observer, menuCloser)
    }

  private def toSlot(equipmentType: EquipmentType): EquipmentSlot =
    equipmentType match {
      case EquipmentType.Head => EquipmentSlot.Head
      case EquipmentType.Cape => EquipmentSlot.Cape
      case EquipmentType.Neck => EquipmentSlot.Neck
      case EquipmentType.Ammo => EquipmentSlot.Ammo
      case EquipmentType.Weapon => EquipmentSlot.Weapon
      case EquipmentType.Shield => EquipmentSlot.Shield
      case EquipmentType.TwoHanded => EquipmentSlot.Weapon
      case EquipmentType.Body => EquipmentSlot.Body
      case EquipmentType.Legs => EquipmentSlot.Legs
      case EquipmentType.Hands => EquipmentSlot.Hands
      case EquipmentType.Feet => EquipmentSlot.Feet
      case EquipmentType.Ring => EquipmentSlot.Ring
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
        Set(toSlot(other) -> Set(other))
    }

  private def removeButton(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[GainItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Button] = {
    val observer =
      if (heldQuantity > 1)
        toRemoveItemFormOpener(item, heldQuantity, effectObserver, modalBus)
      else
        effectObserver.contramap[Unit](_ => GainItem(item.id, -heldQuantity, inventory))

    button("Remove", observer, menuCloser)
  }

  private def toRemoveItemFormOpener(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[GainItem],
    modalBus: WriteBus[Option[L.Element]]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = RemoveItemForm(item, heldQuantity, inventory)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

  private def button(
    text: String,
    clickObserver: Observer[Unit],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      L.span(text),
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(clickObserver, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
}
