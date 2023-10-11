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
      toGainItemFormOpener(itemFuse, effectObserverSignal, modalBus),
      effectObserverSignal,
      contextMenuController
    )

  private def toGainItemFormOpener(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    modalBus: WriteBus[Option[L.Element]]
  ): Signal[Observer[FormOpener.Command]] =
    effectObserverSignal.map { maybeObserver =>
      val (form, formSubmissions) = GainItemForm(Depository.Kind.Inventory, itemFuse)
      FormOpener(
        modalBus,
        maybeObserver.observer,
        () => (form, formSubmissions.collect { case Some(effect) => effect })
      )
    }

  private def toMenuBinder(
    gainItemFormOpenerSignal: Signal[Observer[FormOpener.Command]],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(effectObserverSignal, gainItemFormOpenerSignal)
        .map { case (maybeEffectObserver, gainItemFormOpener) =>
          Option.when(maybeEffectObserver.nonEmpty)(
            toElement(gainItemFormOpener, menuCloser)
          )
        }
    )

  private def toElement(
    gainItemFormOpener: Observer[FormOpener.Command],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      L.span("Add item"),
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(gainItemFormOpener, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
}
