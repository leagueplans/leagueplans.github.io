package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object MoveItemForm {
  def apply(
    stack: ItemStack,
    source: Depository.Kind,
    target: Depository.Kind,
    noteInTarget: Boolean
  ): (L.FormElement, EventStream[Option[MoveItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityInput, quantityLabel, quantitySignal) = toQuantityInput(stack.quantity)

    val form = emptyForm.amend(L.padding("1rem"), quantityLabel, quantityInput, submitButton)
    (form, effectSubmissions(stack, source, target, noteInTarget, quantitySignal, formSubmissions))
  }

  private def toQuantityInput(heldQuantity: Int): (L.Input, L.Label, Signal[Int]) = {
    val (input, label, quantitySignal) =
      NumberInput(id = "move-item-quantity-input", initial = heldQuantity)

    val amendedInput = input.amend(
      L.required(true),
      L.minAttr("1"),
      L.maxAttr(heldQuantity.toString),
      L.stepAttr("1"),
      L.selectOnFocus
    )
    val amendedLabel = label.amend("Quantity:")

    (amendedInput, amendedLabel, quantitySignal)
  }

  private def effectSubmissions(
    stack: ItemStack,
    source: Depository.Kind,
    target: Depository.Kind,
    noteInTarget: Boolean,
    quantitySignal: Signal[Int],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[MoveItem]] =
    formSubmissions.sample(quantitySignal).map(quantity =>
      Option.when(quantity > 0)(
        MoveItem(
          stack.item.id,
          quantity,
          source,
          stack.noted,
          target,
          noteInTarget
        )
      )
    )
}
