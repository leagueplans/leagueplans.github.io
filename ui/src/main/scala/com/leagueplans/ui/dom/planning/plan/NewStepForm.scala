package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{FormOpener, Modal}
import com.leagueplans.ui.dom.common.form.{Form, TextInput}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.LaminarOps.selectOnFocus
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewStepForm {
  def apply(forester: Forester[Step.ID, Step], modal: Modal): NewStepForm = {
    val (form, submitButton, formSubmissions) = Form()
    val (input, label, descriptionSignal) = createInput()

    form.amend(
      L.cls(Styles.form),
      L.p(L.cls(Styles.title), "Add a new step"),
      label,
      input,
      explainer(),
      submitButton.amend(
        L.cls(Styles.submit),
        L.value("Add step"),
        L.disabled <-- descriptionSignal.map(_.isEmpty)
      )
    )

    new NewStepForm(
      forester,
      modal,
      form,
      stepSubmissions(formSubmissions, descriptionSignal)
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

  private def createInput(): (L.Input, L.Label, Signal[String]) = {
    val (input, label, description) = TextInput(
      TextInput.Type.Text,
      id = "new-step-description",
      initial = ""
    )

    input.amend(
      L.cls(Styles.input),
      L.required(true),
      L.placeholder("Chop some logs"),
      L.selectOnFocus
    )
    label.amend(L.cls(Styles.label), "Enter a description")

    (input, label, description)
  }

  private def explainer(): ReactiveHtmlElement[Paragraph] =
    L.p(
      L.cls(Styles.explainer),
      "Plans are built from collections of steps. Once you've created a step, you can click on it to" +
        " set it as the focused step. Any effects you create, like gaining experience in a skill, will" +
        " be tracked against the focused step."
    )

  private def stepSubmissions(
    submissions: EventStream[Unit],
    descriptionSignal: Signal[String]
  ): EventStream[Option[Step]] =
    submissions
      .sample(descriptionSignal)
      .map(description => Option.when(description.nonEmpty)(Step(description)))
}

final class NewStepForm private(
  forester: Forester[Step.ID, Step],
  modal: Modal,
  form: L.FormElement,
  submissions: EventStream[Option[Step]]
) {
  private var maybeParent = Option.empty[Step.ID]
  private val opener = FormOpener(
    modal,
    form,
    submissions,
    _.foreach(forester.add(_, maybeParent))
  )

  def open(maybeParent: Option[Step.ID]): Unit = {
    this.maybeParent = maybeParent
    opener.open()
  }
}
