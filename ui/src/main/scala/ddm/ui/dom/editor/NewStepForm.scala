package ddm.ui.dom.editor

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToNode}
import ddm.ui.dom.common.form.{Form, TextInput}
import ddm.ui.model.plan.{EffectList, Step}

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
      label.amend(L.span("Description:")),
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
        case description => Some(Step(description, EffectList.empty))
      }
}
