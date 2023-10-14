package ddm.ui.dom.player.item.equipment

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Div

object EquippedItemContextMenu {
  def apply(
    item: Item,
    stackSizeSignal: Signal[Int],
    slot: EquipmentSlot,
    effectObserverSignal: Signal[Option[Observer[MoveItem]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, stackSizeSignal)
        .map { case (maybeEffectObserver, stackSize) =>
          maybeEffectObserver.map(effectObserver =>
            toMenu(item, stackSize, slot, effectObserver, menuCloser)
          )
        }
    )

  private def toMenu(
    item: Item,
    stackSize: Int,
    slot: EquipmentSlot,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Div] =
    L.div(
      unequipButton(item, stackSize, slot, effectObserver, menuCloser)
    )

  private def unequipButton(
    item: Item,
    stackSize: Int,
    slot: EquipmentSlot,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Child = {
    val observer = effectObserver.contramap[Unit](_ =>
      MoveItem(
        item.id,
        stackSize,
        slot,
        notedInSource = false,
        Depository.Kind.Inventory,
        noteInTarget = false
      )
    )

    L.button(
      L.`type`("button"),
      "Unequip",
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(observer, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
  }
}
