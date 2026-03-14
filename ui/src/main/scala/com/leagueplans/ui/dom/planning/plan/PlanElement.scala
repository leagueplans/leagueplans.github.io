package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.FocusContext
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
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    tooltip: Tooltip,
    contextMenu: ContextMenu,
    modal: Modal
  ): L.Div = {
    val newStepForm = NewStepForm(forester, modal)
    val deleteStepForm = DeleteStepForm(forester, focusController, tooltip, modal)

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
        tooltip,
        contextMenu,
        focusController
      ).amend(L.cls(Styles.steps)),
      HotkeyModifiers(focusContext.focusID, focusController, newStepForm, deleteStepForm)
    )
  }

  @js.native @JSImport("/styles/planning/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val header: String = js.native
    val steps: String = js.native
  }
}
