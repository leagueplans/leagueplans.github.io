package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.common.model.Item.Bankable
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.dom.player.item.MoveItemForm
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Div

object BankItemContextMenu {
  def apply(
    item: Item,
    stackSizeSignal: Signal[Int],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map { maybeEffectObserver =>
        maybeEffectObserver.map(effectObserver =>
          toMenu(item, stackSizeSignal, effectObserver, menuCloser, modalBus)
        )
      }
    )

  private def toMenu(
    item: Item,
    stackSizeSignal: Signal[Int],
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Div] =
    L.div(
      L.child <-- stackSizeSignal.map(withdrawButton(item, _, effectObserver, menuCloser, modalBus))
    )

  private def withdrawButton(
    item: Item,
    stackSize: Int,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): L.Child =
    item.bankable match {
      case Bankable.No => L.emptyNode
      case _: Bankable.Yes =>
        val observer =
          if (stackSize > 1)
            toWithdrawItemFormOpener(item, stackSize, effectObserver, modalBus)
          else
            effectObserver.contramap[Unit](_ =>
              MoveItem(item.id, stackSize, Depository.Kind.Bank, Depository.Kind.Inventory)
            )

        L.button(
          L.`type`("button"),
          L.span("Withdraw"),
          L.ifUnhandled(L.onClick) -->
            Observer
              .combine(observer, menuCloser)
              .contramap[MouseEvent](_.preventDefault())
        )
    }

  private def toWithdrawItemFormOpener(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[MoveItem],
    modalBus: WriteBus[Option[L.Element]]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(item, heldQuantity, Depository.Kind.Bank, Depository.Kind.Inventory)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }
}
