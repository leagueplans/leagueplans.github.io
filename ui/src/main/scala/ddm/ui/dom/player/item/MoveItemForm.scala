package ddm.ui.dom.player.item

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.form.{Form, NumberInput}
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository

object MoveItemForm {
  def apply(
    item: Item,
    heldQuantity: Int,
    source: Depository.Kind,
    target: Depository.Kind
  ): (L.FormElement, EventStream[Option[MoveItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityInput, quantityLabel, quantitySignal) = toQuantityInput(heldQuantity)

    val form = emptyForm.amend(quantityLabel, quantityInput, submitButton)
    (form, effectSubmissions(item, source, target, quantitySignal, formSubmissions))
  }

  private def toQuantityInput(heldQuantity: Int): (L.Input, L.Label, Signal[Int]) = {
    val (input, label, quantitySignal) =
      NumberInput(id = "move-item-quantity-input", initial = heldQuantity)

    val amendedInput = input.amend(
      L.required(true),
      L.minAttr("1"),
      L.maxAttr(heldQuantity.toString),
      L.stepAttr("1")
    )
    val amendedLabel = label.amend(L.span("Quantity:"))

    (amendedInput, amendedLabel, quantitySignal)
  }

  private def effectSubmissions(
    item: Item,
    source: Depository.Kind,
    target: Depository.Kind,
    quantitySignal: Signal[Int],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[MoveItem]] =
    formSubmissions.sample(quantitySignal).map(quantity =>
      Option.when(quantity > 0)(MoveItem(item.id, quantity, source, target))
    )
}
