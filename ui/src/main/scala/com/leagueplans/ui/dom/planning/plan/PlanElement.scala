package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.client.PlanSubscription
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//TODO Context menus
//TODO Hotkeys
//TODO Cross plan copy/paste
object PlanElement {
  def apply(
    planName: String,
    forester: Forester[Step.ID, Step],
    subscription: PlanSubscription,
    editingEnabled: Signal[Boolean],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    contextMenuController: ContextMenu.Controller,
    focusController: FocusedStep.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Div =
    L.div(
      L.cls(Styles.plan),
      PlanHeader(
        planName,
        forester,
        focusController,
        modalController,
      ).amend(L.cls(Styles.header)),
      StepsElement(
        forester,
        subscription,
        editingEnabled,
        stepsWithErrorsSignal,
        contextMenuController,
        focusController,
        toastPublisher
      ).amend(L.cls(Styles.steps))
    )

  @js.native @JSImport("/styles/planning/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val header: String = js.native
    val steps: String = js.native
  }
}
