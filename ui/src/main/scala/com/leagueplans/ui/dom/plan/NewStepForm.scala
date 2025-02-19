package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.dom.common.form.{Form, TextInput}
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

//TODO Revisit styling
object NewStepForm {
  def apply(): (L.FormElement, EventStream[Option[Step]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (input, label, descriptionSignal) = descriptionInput()
    val form = emptyForm.amend(
      label,
      input,
      submitButton
    )

    (form, stepSubmissions(formSubmissions, descriptionSignal))
  }

  private def descriptionInput(): (L.Input, L.Label, Signal[String]) = {
    val (descriptionInput, label, description) = TextInput(
      TextInput.Type.Text,
      id = "new-step-description",
      initial = ""
    )

    (
      descriptionInput.amend(L.placeholder("Chop some logs")),
      label.amend("Description:"),
      description
    )
  }

  private def stepSubmissions(
    formSubmissions: EventStream[Unit],
    descriptionSignal: Signal[String]
  ): EventStream[Option[Step]] =
    formSubmissions
      .sample(descriptionSignal)
      .map {
        case "" => None
        case description => Some(Step(description))
      }
}
