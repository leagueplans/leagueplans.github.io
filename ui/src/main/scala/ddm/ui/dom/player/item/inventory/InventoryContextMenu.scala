package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.Depository
import ddm.ui.utils.airstream.ObserverOps.RichOptionObserver
import ddm.ui.utils.laminar.LaminarOps.RichL
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Button

object InventoryContextMenu {
  def apply(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    toMenuBinder(
      toAddItemFormOpener(itemFuse, effectObserverSignal, modalBus),
      effectObserverSignal,
      contextMenuController
    )

  private def toAddItemFormOpener(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    modalBus: WriteBus[Option[L.Element]]
  ): Signal[Observer[FormOpener.Command]] =
    effectObserverSignal.map { maybeObserver =>
      val (form, formSubmissions) = AddItemForm(Depository.Kind.Inventory, itemFuse, modalBus)
      FormOpener(
        modalBus,
        maybeObserver.observer,
        () => (form, formSubmissions.collect { case Some(effect) => effect })
      )
    }

  private def toMenuBinder(
    addItemFormOpenerSignal: Signal[Observer[FormOpener.Command]],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, addItemFormOpenerSignal)
        .map { case (maybeEffectObserver, addItemFormOpener) =>
          Option.when(maybeEffectObserver.nonEmpty)(
            toElement(addItemFormOpener, menuCloser)
          )
        }
    )

  private def toElement(
    addItemFormOpener: Observer[FormOpener.Command],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      "Add item",
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(addItemFormOpener, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
}
