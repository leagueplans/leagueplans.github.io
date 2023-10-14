package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, eventPropToProcessor, optionToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.dom.player.item.MoveItemForm
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.{Depository, Stack}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent
import org.scalajs.dom.html.Div

object BankItemContextMenu {
  def apply(
    item: Item,
    stackSize: Int,
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Div] =
    L.div(
      withdrawButton(item, stackSize, note = false, effectObserver, menuCloser, modalBus),
      Option.when(item.noteable)(
        withdrawButton(item, stackSize, note = true, effectObserver, menuCloser, modalBus)
      )
    )

  private def withdrawButton(
    item: Item,
    stackSize: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): L.Child = {
    val observer =
      if (stackSize > 1)
        toWithdrawItemFormOpener(item, stackSize, note, effectObserver, modalBus)
      else
        effectObserver.contramap[Unit](_ =>
          MoveItem(
            item.id,
            stackSize,
            Depository.Kind.Bank,
            notedInSource = false,
            Depository.Kind.Inventory,
            noteInTarget = note
          )
        )

    L.button(
      L.`type`("button"),
      if (note) "Withdraw noted" else "Withdraw",
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(observer, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
  }

  private def toWithdrawItemFormOpener(
    item: Item,
    heldQuantity: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    modalBus: WriteBus[Option[L.Element]]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(
      Stack(item, noted = false),
      heldQuantity,
      Depository.Kind.Bank,
      Depository.Kind.Inventory,
      note
    )
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }
}
