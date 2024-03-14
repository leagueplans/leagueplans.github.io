package ddm.ui.dom.player.item.equipment

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Val
import com.raquo.laminar.api.L
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.model.player.item.Stack

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EquipmentSlotElement {
  def apply(
    slot: EquipmentSlot,
    stacks: List[(Stack, List[Int])],
    effectObserverSignal: Signal[Option[Observer[MoveItem]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.slot),
      L.img(
        L.cls(Styles.background),
        L.src(toBackground(slot, stacks.isEmpty)),
        L.alt(slot.name)
      ),
      StackList(
        Val(stacks),
        (stack, stackSizeSignal) => StackElement(stack, stackSizeSignal).amend(
          bindContextMenu(stack.item, stackSizeSignal, slot, effectObserverSignal, contextMenuController)
        )
      ).amend(L.cls(Styles.contents))
    )

  private object Backgrounds {
    @js.native @JSImport("/images/equipment/filled-slot.png", JSImport.Default)
    val filled: String = js.native
    @js.native @JSImport("/images/equipment/ammo-slot.png", JSImport.Default)
    val ammo: String = js.native
    @js.native @JSImport("/images/equipment/body-slot.png", JSImport.Default)
    val body: String = js.native
    @js.native @JSImport("/images/equipment/cape-slot.png", JSImport.Default)
    val cape: String = js.native
    @js.native @JSImport("/images/equipment/feet-slot.png", JSImport.Default)
    val feet: String = js.native
    @js.native @JSImport("/images/equipment/hands-slot.png", JSImport.Default)
    val hands: String = js.native
    @js.native @JSImport("/images/equipment/head-slot.png", JSImport.Default)
    val head: String = js.native
    @js.native @JSImport("/images/equipment/legs-slot.png", JSImport.Default)
    val legs: String = js.native
    @js.native @JSImport("/images/equipment/neck-slot.png", JSImport.Default)
    val neck: String = js.native
    @js.native @JSImport("/images/equipment/ring-slot.png", JSImport.Default)
    val ring: String = js.native
    @js.native @JSImport("/images/equipment/shield-slot.png", JSImport.Default)
    val shield: String = js.native
    @js.native @JSImport("/images/equipment/weapon-slot.png", JSImport.Default)
    val weapon: String = js.native
  }

  @js.native @JSImport("/styles/player/item/equipment/equipmentSlotElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val slot: String = js.native
    val contents: String = js.native
    val background: String = js.native
  }

  private def toBackground(slot: EquipmentSlot, isEmpty: Boolean): String =
    if (!isEmpty)
      Backgrounds.filled
    else
      slot match {
        case EquipmentSlot.Head => Backgrounds.head
        case EquipmentSlot.Cape => Backgrounds.cape
        case EquipmentSlot.Neck => Backgrounds.neck
        case EquipmentSlot.Ammo => Backgrounds.ammo
        case EquipmentSlot.Weapon => Backgrounds.weapon
        case EquipmentSlot.Shield => Backgrounds.shield
        case EquipmentSlot.Body => Backgrounds.body
        case EquipmentSlot.Legs => Backgrounds.legs
        case EquipmentSlot.Hands => Backgrounds.hands
        case EquipmentSlot.Feet => Backgrounds.feet
        case EquipmentSlot.Ring => Backgrounds.ring
      }

  private def bindContextMenu(
    item: Item,
    stackSizeSignal: Signal[Int],
    slot: EquipmentSlot,
    effectObserverSignal: Signal[Option[Observer[MoveItem]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, stackSizeSignal)
        .map((maybeEffectObserver, stackSize) =>
          maybeEffectObserver.map(effectObserver =>
            EquippedItemContextMenu(item, stackSize, slot, effectObserver, menuCloser)
          )
        )
    )
}
