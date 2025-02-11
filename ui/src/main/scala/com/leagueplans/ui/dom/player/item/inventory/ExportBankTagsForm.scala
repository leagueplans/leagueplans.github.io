package com.leagueplans.ui.dom.player.item.inventory

import com.leagueplans.ui.dom.common.form.{Form, TextInput}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object ExportBankTagsForm {
  def apply(): (L.FormElement, EventStream[Option[String]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (input, label, signal) = createNameInput()
    val form = emptyForm.amend(label, input, submitButton)
    (form, combine(signal, formSubmissions))
  }
  
  private def createNameInput(): (L.Input, L.Label, Signal[String]) = {
    val (baseInput, baseLabel, signal) = TextInput(TextInput.Type.Text, id = "bank-tag-name-input", initial = "")
    val input = baseInput.amend(L.required(true))
    val label = baseLabel.amend("Tag name:")
    (input, label, signal)
  }
  
  private def combine(
    nameSignal: Signal[String],
    formSubmissions: EventStream[Unit]
  ): EventStream[Option[String]] =
    formSubmissions.sample(
      nameSignal.map(name =>
        Option.when(name.nonEmpty)(name)
      )
    )
}
