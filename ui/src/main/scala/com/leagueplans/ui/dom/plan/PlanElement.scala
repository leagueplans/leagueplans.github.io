package com.leagueplans.ui.dom.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub}
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.dom.plan.step.StepElement
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanElement {
  def apply(
    initialPlan: Plan,
    focusedStep: Var[Option[Step.ID]],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher,
    stepUpdates: EventBus[Forester[Step.ID, Step] => Unit],
  ): (L.Div, Forester[Step.ID, Step]) = {
    val allStepsVar = Var(List.empty[Step.ID])
    val completionManager = CompletionManager(allStepsVar.signal)
    val clipboard = Clipboard[Step]("step", toastPublisher, Decoder.decodeMessage)

    val forester = Forester(
      initialPlan.steps,
      toElement(
        _,
        _,
        _,
        focusedStep,
        completionManager,
        stepsWithErrorsSignal,
        editingEnabled,
        contextMenuController,
        clipboard,
        stepUpdates.writer
      )
    )

    val dom =
      L.div(
        L.cls(Styles.plan),
        PlanHeader(
          initialPlan.name,
          focusedStep, 
          modalController,
          stepUpdates.writer
        ).amend(L.cls(Styles.header)),
        L.div(
          L.cls(Styles.steps),
          L.children <-- forester.domSignal
        ),
        stepUpdates.events --> (_.apply(forester)),
        forester.forestSignal.map(_.toList.map(_.id)) --> allStepsVar
      )

    (dom, forester)
  }

  @js.native @JSImport("/styles/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val header: String = js.native
    val steps: String = js.native
  }

  private def toElement(
    stepID: Step.ID,
    step: Signal[Step],
    substeps: Signal[List[L.HtmlElement]],
    focusedStep: Var[Option[Step.ID]],
    completionManager: CompletionManager,
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): L.HtmlElement =
    StepElement(
      stepID,
      step,
      substeps,
      focusedStep,
      isCompleteSignal = completionManager.isCompleteSignal(stepID),
      hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)),
      editingEnabled,
      contextMenuController,
      clipboard,
      stepUpdater,
      completionStatusObserver = Observer[Boolean](completionManager.updateStatus(stepID, _))
    )
}
