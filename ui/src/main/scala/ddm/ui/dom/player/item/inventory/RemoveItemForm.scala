package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.form.{Form, NumberInput}
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.item.Depository

object RemoveItemForm {
  def apply(
    item: Item,
    heldQuantity: Int,
    depository: Depository.Kind
  ): (L.FormElement, EventStream[Option[GainItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (input, label, quantitySignal) = quantityInput(heldQuantity)
    val form = emptyForm.amend(label, input, submitButton)
    (form, effectSubmissions(item, depository, quantitySignal, formSubmissions))
  }

  private def quantityInput(heldQuantity: Int): (L.Input, L.Label, Signal[Int]) = {
    val (input, label, quantitySignal) =
      NumberInput(id = "remove-item-quantity-input", initial = heldQuantity)

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
    depository: Depository.Kind,
    quantitySignal: Signal[Int],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[GainItem]] =
    formSubmissions
      .sample(quantitySignal)
      .map(quantity => Option.when(quantity > 0)(
        GainItem(item.id, -quantity, depository)
      ))
}
