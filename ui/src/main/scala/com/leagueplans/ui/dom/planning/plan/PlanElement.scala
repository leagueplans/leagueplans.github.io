package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.editor.description.EditStepDescriptionForm
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.FocusContext
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.uicommon.dom.{ContextMenu, Modal, ToastHub, Tooltip}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanElement {
  def apply(
    planName: String,
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    focusController: FocusController,
    collapsedSteps: CollapsedSteps,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    timeKeeper: TimeKeeper,
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val newStepForm = NewStepForm(forester, modal)
    val deleteStepForm = DeleteStepForm(forester, focusController, tooltip, modal)
    val stepClipboard = StepClipboard(forester, toastPublisher)
    val stepMover = StepMover(forester)

    L.div(
      L.cls(Styles.plan),
      PlanHeader(planName, focusContext.focusID, tooltip, modal, newStepForm, deleteStepForm).amend(
        L.cls(Styles.header)
      ),
      InteractiveForest(
        forester,
        focusContext,
        editingEnabled,
        stepsWithErrorsSignal,
        timeKeeper,
        tooltip,
        contextMenu,
        focusController,
        collapsedSteps,
        stepMover,
        stepClipboard
      ).amend(L.cls(Styles.steps)),
      HotkeyModifiers(
        focusContext.focusID,
        focusController,
        stepMover,
        stepClipboard,
        newStepForm,
        deleteStepForm,
        editDescription = step =>
          forester.signal.now().get(step).foreach(EditStepDescriptionForm.open(_, forester, modal))
      )
    )
  }

  @js.native @JSImport("/styles/planning/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val header: String = js.native
    val steps: String = js.native
  }
}
