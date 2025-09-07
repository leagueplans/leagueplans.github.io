package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.form.Form
import com.leagueplans.ui.dom.common.{CancelModalButton, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.DeleteStepForm.Styles
import com.leagueplans.ui.dom.planning.plan.step.StepPreview
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, optionToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DeleteStepForm {
  @js.native @JSImport("/styles/planning/plan/deleteStepForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val title: String = js.native
    val disclaimer: String = js.native
    val preview: String = js.native
    val cancel: String = js.native
    val confirm: String = js.native
  }
}

final class DeleteStepForm(
  forester: Forester[Step.ID, Step],
  focusController: FocusedStep.Controller,
  modal: Modal
) {
  def open(step: Step.ID): Unit =
    FormOpener(
      modal,
      toForm(forester.signal.now().subforest(step)),
      _ => {
        focusController.next(ignoreChildren = true)
        forester.remove(step)
      }
    ).open()

  private def toForm(steps: Forest[Step.ID, Step]): (L.FormElement, EventStream[Unit]) = {
    val root = steps.roots.headOption.flatMap(steps.nodes.get)
    val (form, submitButton, submissions) = Form()
    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(
        L.cls(Styles.title, Modal.Styles.title),
        if (steps.nodes.size > 1)
          "Are you sure you want to delete these steps?"
        else
          "Are you sure you want to delete this step?"
      ),
      L.p(L.cls(Styles.disclaimer), "This cannot be undone"),
      root.map(
        StepPreview(
          _,
          steps,
          headerOffset = Signal.fromValue(0)
        ).amend(L.cls(Styles.preview))
      ),
      CancelModalButton(modal).amend(
        L.cls(Styles.cancel, Modal.Styles.confirmationButton),
        L.onMountFocus
      ),
      submitButton.amend(
        L.cls(Styles.confirm, Modal.Styles.deletionButton),
        L.value(if (steps.nodes.size > 1) "Delete steps" else "Delete step")
      )
    )

    (form, submissions)
  }
}
