package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.DeleteStepForm.Styles
import com.leagueplans.ui.dom.planning.plan.step.StepPreview
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.form.Form
import com.leagueplans.uicommon.dom.{CancelModalButton, FormOpener, Modal, Tooltip}
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
  focusController: FocusController,
  tooltip: Tooltip,
  modal: Modal
) {
  /** Asks for confirmation before deleting a step with substeps. Deleting a single step can be
    * undone easily enough that it doesn't need confirming. */
  def open(step: Step.ID): Unit = {
    val steps = forester.signal.now().subtree(step)
    if (steps.size <= 1)
      delete(step)
    else
      FormOpener(modal, toForm(steps), _ => delete(step)).open()
  }

  private def delete(step: Step.ID): Unit = {
    focusController.moveOutOf(step)
    forester.remove(step)
  }

  private def toForm(steps: Forest[Step.ID, Step]): (L.FormElement, EventStream[Unit]) = {
    val root = steps.roots.headOption.flatMap(steps.get)
    val (form, submitButton, submissions) = Form()
    form.amend(
      L.cls(Styles.form, Modal.Styles.form),
      L.p(
        L.cls(Styles.title, Modal.Styles.title),
        "Are you sure you want to delete these steps?"
      ),
      L.p(L.cls(Styles.disclaimer), "You can undo this with Ctrl+Z"),
      root.map(
        StepPreview(
          _,
          steps,
          headerOffset = Signal.fromValue(0),
          tooltip
        ).amend(L.cls(Styles.preview))
      ),
      CancelModalButton(modal).amend(
        L.cls(Styles.cancel, Modal.Styles.confirmationButton),
        L.onMountFocus
      ),
      submitButton.amend(
        L.cls(Styles.confirm, Modal.Styles.deletionButton),
        L.value("Delete steps")
      )
    )

    (form, submissions)
  }
}
