package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.form.{CheckboxInput, Form, NumberInput}
import com.leagueplans.ui.dom.common.{CancelModalButton, InfoIcon, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.player.item.ItemSearch
import com.leagueplans.ui.model.plan.Effect.AddItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, enrichSource, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object AddItemForm {
  def apply(
    target: Depository.Kind,
    items: Fuse[Item],
    modal: Modal
  ): (L.FormElement, EventStream[Option[AddItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (quantityNodes, quantitySignal) = quantityInput()
    val (noteNodes, noteSignal) = noteInput()
    val (itemSearchNodes, radioNodes, itemSignal) = itemInput(items, noteSignal, quantitySignal)

    val form = emptyForm.amend(
      L.cls(Styles.form),
      L.div(
        L.cls(Styles.inputs),
        itemSearchNodes,
        quantityNodes,
        noteNodes
      ),
      L.div(L.cls(Styles.radios), radioNodes),
      L.div(
        L.cls(Styles.modalButtons),
        submitButton.amend(L.cls(Styles.modalButton)),
        CancelModalButton(modal).amend(L.cls(Styles.modalButton))
      )
    )
    val submissions = effectSubmissions(target, formSubmissions, itemSignal, quantitySignal, noteSignal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/addItemForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native

    val inputs: String = js.native
    val radios: String = js.native
    val modalButtons: String = js.native

    val label: String = js.native
    val input: String = js.native
    val modalButton: String = js.native

    val infoIcon: String = js.native
    val checkbox: String = js.native
  }

  private def quantityInput(): (L.Modifier[L.Element], Signal[Int])  = {
    val (quantityInput, quantityLabel, quantitySignal) =
      NumberInput(id = "gain-item-quantity-input", initial = 1)

    val nodes = List(
      quantityLabel.amend(L.cls(Styles.label), "Quantity:"),
      quantityInput.amend(
        L.cls(Styles.input),
        L.required(true),
        L.minAttr("1"),
        L.maxAttr(Int.MaxValue.toString),
        L.stepAttr("1")
      )
    )

    (nodes, quantitySignal)
  }

  private def itemInput(
    items: Fuse[Item],
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int]
  ): (L.Modifier[L.Element], L.Modifier[L.Element], Signal[Option[Item]]) = {
    val (itemSearch, itemSearchLabel, radios, itemSignal) =
      ItemSearch(items, noteSignal, quantitySignal, id = "gain-item-item-search")

    val searchNodes = List(
      itemSearchLabel.amend(L.cls(Styles.label)),
      itemSearch.amend(
        L.cls(Styles.input),
        L.required(true),
        L.inContext(node =>
          itemSignal --> {
            case Some(_) => node.ref.setCustomValidity("")
            case None => node.ref.setCustomValidity("Please enter a valid item name")
          }
        )
      )
    )

    (searchNodes, radios, itemSignal)
  }

  private def noteInput(): (L.Modifier[L.Element], Signal[Boolean]) = {
    val (checkbox, label, signal) = CheckboxInput("gain-item-note-checkbox", initial = false)

    val nodes = List(
      label.amend(
        L.cls(Styles.label),
        InfoIcon().amend(L.svg.cls(Styles.infoIcon)),
        Tooltip(L.p("This input will be ignored for items that cannot be noted.")),
        "Note:"
      ),
      checkbox.amend(L.cls(Styles.checkbox))
    )

    (nodes, signal)
  }

  private def effectSubmissions(
    target: Depository.Kind,
    formSubmissions: EventStream[Unit],
    itemSignal: Signal[Option[Item]],
    quantitySignal: Signal[Int],
    noteSignal: Signal[Boolean]
  ): EventStream[Option[AddItem]] =
    formSubmissions
      .sample(Signal.combine(itemSignal, quantitySignal, noteSignal))
      .map {
        case (Some(item), quantity, note) if quantity > 0 =>
          Some(AddItem(item.id, quantity, target, note && item.noteable))
        case _ =>
          None
      }
}
