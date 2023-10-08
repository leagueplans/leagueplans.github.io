package ddm.ui.dom.player.item

import cats.data.NonEmptyList
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToNode}
import ddm.common.model.Item
import ddm.common.model.Item.Bankable
import ddm.ui.dom.common.form.{Form, NumberInput, Select}
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository

object MoveItemForm {
  def apply(
    item: Item,
    heldQuantity: Int,
    heldDepository: Depository.Kind
  ): (L.FormElement, EventStream[Option[MoveItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityInput, quantityLabel, quantitySignal) = toQuantityInput(heldQuantity)
    val (targetInput, targetLabel, targetSignal) = toTargetInput(item, heldDepository)

    val form = emptyForm.amend(
      quantityLabel,
      quantityInput,
      targetLabel,
      targetInput,
      submitButton
    )

    (form, effectSubmissions(item, heldDepository, quantitySignal, targetSignal, formSubmissions))
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

  private def toTargetInput(
    item: Item,
    heldDepository: Depository.Kind
  ): (L.Select, L.Label, Signal[Depository.Kind]) = {
    val bankableFilter = Option.when(item.bankable match {
      case _: Bankable.Yes => false
      case Bankable.No => true
    })(Depository.Kind.Bank)

    val viableTargets = Depository.Kind.kinds - heldDepository -- bankableFilter

    val (input, label, targetSignal) =
      Select(
        id = "move-item-target-input",
        NonEmptyList.fromListUnsafe(
          viableTargets.toList.sorted.map(depository =>
            Select.Opt(depository, depository.name)
          )
        )
      )

    (input, label.amend(L.span("Move to:")), targetSignal)
  }

  private def effectSubmissions(
    item: Item,
    heldDepository: Depository.Kind,
    quantitySignal: Signal[Int],
    targetDepositorySignal: Signal[Depository.Kind],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[MoveItem]] =
    formSubmissions
      .sample(Signal.combine(quantitySignal, targetDepositorySignal))
      .map { case (quantity, targetDepository) =>
        Option.when(targetDepository != heldDepository)(
          MoveItem(item.id, quantity, source = heldDepository, target = targetDepository)
        )
      }
}
