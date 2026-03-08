package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.resolution.FocusContext
import com.leagueplans.ui.storage.client.PlanSubscription
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//TODO Context menus
//TODO Cross plan copy/paste
object PlanElement {
  def apply(
    planName: String,
    forester: Forester[Step.ID, Step],
    focusContext: FocusContext,
    subscription: PlanSubscription,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller,
    focusController: FocusController,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
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
        subscription,
        editingEnabled,
        stepsWithErrorsSignal,
        tooltip,
        contextMenuController,
        focusController,
        toastPublisher
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
