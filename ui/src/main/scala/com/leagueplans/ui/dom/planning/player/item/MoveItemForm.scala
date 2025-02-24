package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.dom.common.form.{Form, NumberInput}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.{Depository, Stack}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object MoveItemForm {
  def apply(
    stack: Stack,
    heldQuantity: Int,
    source: Depository.Kind,
    target: Depository.Kind,
    noteInTarget: Boolean
  ): (L.FormElement, EventStream[Option[MoveItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityInput, quantityLabel, quantitySignal) = toQuantityInput(heldQuantity)

    val form = emptyForm.amend(quantityLabel, quantityInput, submitButton)
    (form, effectSubmissions(stack, source, target, noteInTarget, quantitySignal, formSubmissions))
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
    val amendedLabel = label.amend("Quantity:")

    (amendedInput, amendedLabel, quantitySignal)
  }

  private def effectSubmissions(
    stack: Stack,
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
