package com.leagueplans.ui.dom.planning.editor.description

import com.leagueplans.ui.dom.common.form.Form
import com.leagueplans.ui.dom.common.{CancelModalButton, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.common.StepDescriptionInput
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditStepDescriptionForm {
  def open(
    step: Step,
    forester: Forester[Step.ID, Step],
    modal: Modal
  ): Unit = {
    val submissions = EventBus[Unit]()
    val (form, submitButton, formSubmissions) = Form()
    val (input, _, description) =
      StepDescriptionInput(id = "edit", initial = step.description)(submissions.writer)

    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "Edit step description"),
      input.amend(L.cls(Styles.input)),
      L.div(
        L.cls(Styles.buttons),
        CancelModalButton(modal).amend(L.cls(Styles.cancel, Modal.Styles.confirmationButton)),
        submitButton.amend(
          L.cls(Styles.confirm, Modal.Styles.confirmationButton),
          L.value("Save")
        )
      ),
      formSubmissions --> submissions
    )

    FormOpener(
      modal,
      form,
      submissions.events.sample(description),
      description => forester.update(step.id, _.deepCopy(description = description))
    ).open()
  }

  @js.native @JSImport("/styles/planning/editor/description/editStepDescriptionForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val input: String = js.native
    val buttons: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }
}
