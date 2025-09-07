package com.leagueplans.ui.dom.planning.player.item.inventory.forms

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.form.{CheckboxInput, Form, NumberInput}
import com.leagueplans.ui.dom.common.{CancelModalButton, InfoIcon, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.player.item.ItemSearch
import com.leagueplans.ui.model.plan.Effect.AddItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object AddItemForm {
  def apply(
    target: Depository.Kind,
    items: Fuse[Item],
    modal: Modal
  ): (L.FormElement, EventStream[Option[AddItem]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val quantity = createQuantityInput()
    val note = createNoteInput()
    val item = createItemInput(items, note.signal, quantity.signal)

    val form = emptyForm.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "Add items to the inventory"),
      L.sectionTag(
        L.cls(Styles.inputs),
        item.label,
        item.input,
        quantity.label,
        quantity.input,
        note.label,
        note.input
      ),
      L.div(L.cls(Styles.radios), item.radios),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modal).amend(L.cls(Styles.cancel, Modal.Styles.confirmationButton)),
        submitButton.amend(
          L.cls(Styles.confirm, Modal.Styles.confirmationButton),
          L.value("Add items")
        )
      )
    )
    val submissions = effectSubmissions(target, formSubmissions, item.signal, quantity.signal, note.signal)

    (form, submissions)
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/forms/addItemForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native

    val inputs: String = js.native
    val radios: String = js.native
    val buttons: String = js.native

    val quantityLabel: String = js.native
    val quantityInput: String = js.native

    val itemLabel: String = js.native
    val itemInput: String = js.native

    val infoIcon: String = js.native
    val noteLabel: String = js.native
    val noteInput: String = js.native

    val confirm: String = js.native
    val cancel: String = js.native
  }

  private def createQuantityInput(): (input: L.Input, label: L.Label, signal: Signal[Int])  = {
    val (input, label, signal) = NumberInput(id = "gain-item-quantity-input", initial = 1)

    label.amend(L.cls(Styles.quantityLabel), "How many items should be added?")
    input.amend(
      L.cls(Styles.quantityInput),
      L.required(true),
      L.minAttr("1"),
      L.maxAttr(Int.MaxValue.toString),
      L.stepAttr("1")
    )

    (input, label, signal)
  }

  private def createItemInput(
    items: Fuse[Item],
    noteSignal: Signal[Boolean],
    quantitySignal: Signal[Int]
  ): (input: L.Input, label: L.Label, radios: L.Modifier[L.Element], signal: Signal[Option[Item]]) = {
    val (search, label, radios, signal) =
      ItemSearch(items, noteSignal, quantitySignal, id = "gain-item-item-search")

    label.amend(
      L.cls(Styles.itemLabel),
      "Enter an item name to search for"
    )
    search.amend(
      L.cls(Styles.itemInput),
      L.required(true),
      L.inContext(node =>
        signal --> {
          case Some(_) => node.ref.setCustomValidity("")
          case None => node.ref.setCustomValidity("No items found for the given input")
        }
      )
    )

    (search, label, radios, signal)
  }

  private def createNoteInput(): (input: L.Input, label: L.Label, signal: Signal[Boolean]) = {
    val (checkbox, label, signal) = CheckboxInput("gain-item-note-checkbox", initial = false)

    label.amend(
      L.cls(Styles.noteLabel),
      InfoIcon().amend(L.svg.cls(Styles.infoIcon)),
      Tooltip(L.span("Items that cannot be noted will ignore this setting")),
      "Should the items be noted?"
    )
    checkbox.amend(L.cls(Styles.noteInput))

    (checkbox, label, signal)
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
