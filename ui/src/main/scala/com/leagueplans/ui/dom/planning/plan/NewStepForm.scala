package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.form.Form
import com.leagueplans.ui.dom.common.{FormOpener, Modal}
import com.leagueplans.ui.dom.planning.common.StepDescriptionInput
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewStepForm {
  def apply(forester: Forester[Step.ID, Step], modal: Modal): NewStepForm = {
    val submissions = EventBus[Unit]()
    val (form, submitButton, formSubmissions) = Form()
    val (input, label, description) =
      StepDescriptionInput(id = "new", initial = "")(submissions.writer)

    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(L.cls(Styles.title, Modal.Styles.title), "Add a new step"),
      label.amend(L.cls(Styles.label), "Description"),
      input.amend(L.cls(Styles.input)),
      explainer(),
      submitButton.amend(
        L.cls(Styles.submit, Modal.Styles.confirmationButton),
        L.value("Add step"),
      ),
      formSubmissions --> submissions
    )

    new NewStepForm(
      forester,
      modal,
      form,
      submissions.events.sample(description).map(Step(_))
    )
  }

  @js.native @JSImport("/styles/planning/plan/newStepForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val input: String = js.native
    val label: String = js.native
    val explainer: String = js.native
    val submit: String = js.native
  }

  private def explainer(): ReactiveHtmlElement[Paragraph] =
    L.p(
      L.cls(Styles.explainer),
      "Plans are built from collections of steps. Once you've created a step, you can click on it to" +
        " set it as the focused step. Any effects you create, like gaining experience in a skill, will" +
        " be tracked against the focused step."
    )
}

final class NewStepForm private(
  forester: Forester[Step.ID, Step],
  modal: Modal,
  form: L.FormElement,
  submissions: EventStream[Step]
) {
  private var maybeParent = Option.empty[Step.ID]
  private val opener = FormOpener(modal, form, submissions, forester.add(_, maybeParent))

  def open(maybeParent: Option[Step.ID]): Unit = {
    this.maybeParent = maybeParent
    opener.open()
  }
}
