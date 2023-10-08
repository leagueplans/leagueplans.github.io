package ddm.ui.dom.player.item

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.item.Depository
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Button

object ItemContextMenu {
  def apply(
    item: Item,
    heldQuantitySignal: Signal[Int],
    heldDepository: Depository.Kind,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(closer =>
      Signal.combine(heldQuantitySignal, effectObserverSignal).map {
        case (_, None) => None
        case (heldQuantity, Some(effectObserver)) =>
          Some(
            L.div(
              moveButton(
                item,
                heldQuantity,
                heldDepository,
                effectObserver,
                modalBus,
                closer
              ),
              dropButton(
                item,
                heldQuantity,
                heldDepository,
                effectObserver,
                modalBus,
                closer
              )
            )
          )
      }
    )

  private def moveButton(
    item: Item,
    heldQuantity: Int,
    heldDepository: Depository.Kind,
    effectObserver: Observer[Effect],
    modalBus: WriteBus[Option[L.Element]],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      L.span("Move"),
      L.ifUnhandled(L.onClick) -->
        Observer.combine(
          moveFormOpener(item, heldQuantity, heldDepository, effectObserver, modalBus),
          menuCloser
        ).contramap[MouseEvent](_.preventDefault())
    )

  private def moveFormOpener(
    item: Item,
    heldQuantity: Int,
    heldDepository: Depository.Kind,
    effectObserver: Observer[Effect],
    modalBus: WriteBus[Option[L.Element]],
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(item, heldQuantity, heldDepository)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

  private def dropButton(
    item: Item,
    heldQuantity: Int,
    heldDepository: Depository.Kind,
    effectObserver: Observer[Effect],
    modalBus: WriteBus[Option[L.Element]],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      L.span("Drop"),
      L.ifUnhandled(L.onClick) --> {
        val dropObserver =
          if (heldQuantity > 1)
            dropFormOpener(item, heldQuantity, heldDepository, effectObserver, modalBus)
          else
            effectObserver.contramap[Unit](_ => GainItem(item.id, -1, heldDepository))

        Observer.combine(dropObserver, menuCloser).contramap[MouseEvent](_.preventDefault())
      }
    )

  private def dropFormOpener(
    item: Item,
    heldQuantity: Int,
    heldDepository: Depository.Kind,
    effectObserver: Observer[Effect],
    modalBus: WriteBus[Option[L.Element]],
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = DropItemForm(item, heldQuantity, heldDepository)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }
}
