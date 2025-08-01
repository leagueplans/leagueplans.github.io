package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.model.plan.Effect.AddItem
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object RemoveItemForm {
  def apply(
    stack: ItemStack,
    depository: Depository.Kind
  ): (L.FormElement, EventStream[Option[AddItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (input, label, quantitySignal) = quantityInput(stack.quantity)
    val form = emptyForm.amend(L.padding("1rem"), label, input, submitButton)
    (form, effectSubmissions(stack, depository, quantitySignal, formSubmissions))
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
    val amendedLabel = label.amend("Quantity:")

    (amendedInput, amendedLabel, quantitySignal)
  }

  private def effectSubmissions(
    stack: ItemStack,
    depository: Depository.Kind,
    quantitySignal: Signal[Int],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[AddItem]] =
    formSubmissions
      .sample(quantitySignal)
      .map(quantity => Option.when(quantity > 0)(
        AddItem(stack.item.id, -quantity, depository, stack.noted)
      ))
}
