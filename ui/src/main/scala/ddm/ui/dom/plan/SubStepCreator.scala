package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observable, Observer}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToNode}
import ddm.ui.dom.common.FormOpener
import ddm.ui.dom.common.form.{Form, TextInput}
import ddm.ui.model.plan.{EffectList, Step}

object SubStepCreator {
  def apply(
    modalBus: WriteBus[Option[L.Element]],
    observer: Observer[Step]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalBus,
      observer,
      () => createForm
    )

  private def createForm: (L.FormElement, Observable[Step]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (input, label, description) = TextInput(
      TextInput.Type.Text,
      id = "sub-step-description",
      initial = ""
    )

    val form = emptyForm.amend(
      label.amend(L.span("Description:")),
      input,
      submitButton
    )

    (form, formSubmissions.sample(description.map(desc => Step(desc, EffectList.empty))))
  }
}
