package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, optionToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener, Modal}
import ddm.ui.dom.player.item.MoveItemForm
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.{Depository, Stack}
import ddm.ui.utils.laminar.LaminarOps.*
import org.scalajs.dom.html.Div

object BankItemContextMenu {
  def apply(
    item: Item,
    stackSize: Int,
    effectObserver: Observer[Effect],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalController: Modal.Controller
  ): ReactiveHtmlElement[Div] =
    L.div(
      withdrawButton(item, stackSize, note = false, effectObserver, menuCloser, modalController),
      Option.when(item.noteable)(
        withdrawButton(item, stackSize, note = true, effectObserver, menuCloser, modalController)
      )
    )

  private def withdrawButton(
    item: Item,
    stackSize: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalController: Modal.Controller
  ): L.Node = {
    val observer =
      if (stackSize > 1)
        toWithdrawItemFormOpener(item, stackSize, note, effectObserver, modalController)
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
      L.onClick.handled --> Observer.combine(observer, menuCloser)
    )
  }

  private def toWithdrawItemFormOpener(
    item: Item,
    heldQuantity: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    modalController: Modal.Controller
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = MoveItemForm(
      Stack(item, noted = false),
      heldQuantity,
      Depository.Kind.Bank,
      Depository.Kind.Inventory,
      note
    )
    FormOpener(
      modalController,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }
}
