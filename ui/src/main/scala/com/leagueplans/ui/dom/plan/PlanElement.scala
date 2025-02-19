package com.leagueplans.ui.dom.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.dom.common.{ContextMenu, ToastHub}
import com.leagueplans.ui.dom.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.wrappers.Clipboard
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object PlanElement {
  def apply(
    initialPlan: Forest[Step.ID, Step],
    focusedStep: Signal[Option[Step.ID]],
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    toastPublisher: ToastHub.Publisher,
    stepUpdates: EventBus[Forester[Step.ID, Step] => Unit],
    focusObserver: Observer[Step.ID]
  ): (L.Div, Forester[Step.ID, Step]) = {
    val allStepsVar = Var(List.empty[Step.ID])
    val completionManager = CompletionManager(allStepsVar.signal)
    val clipboard = Clipboard[Step]("step", toastPublisher, Decoder.decodeMessage)

    val forester = Forester(
      initialPlan,
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
        stepUpdates.writer,
        focusObserver
      )
    )

    val dom =
      L.div(
        L.cls(Styles.plan),
        L.children <-- forester.domSignal.map(_.map(_.amend(L.cls(Styles.rootStep)))),
        stepUpdates.events --> (_.apply(forester)),
        forester.forestSignal.map(_.toList.map(_.id)) --> allStepsVar
      ).amend()

    (dom, forester)
  }

  @js.native @JSImport("/styles/plan/plan.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val plan: String = js.native
    val rootStep: String = js.native
  }

  private def toElement(
    stepID: Step.ID,
    step: Signal[Step],
    substeps: Signal[List[L.HtmlElement]],
    focusedStep: Signal[Option[Step.ID]],
    completionManager: CompletionManager,
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    clipboard: Clipboard[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    focusObserver: Observer[Step.ID]
  ): L.HtmlElement =
    StepElement(
      stepID,
      step,
      substeps,
      isFocused = Signal.combine(step, focusedStep).map {
        case (s, Some(focus)) => s.id == focus
        case _ => false
      },
      isCompleteSignal = completionManager.isCompleteSignal(stepID),
      hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)),
      editingEnabled,
      contextMenuController,
      clipboard,
      stepUpdater,
      completionStatusObserver = Observer[Boolean](completionManager.updateStatus(stepID, _)),
      focusObserver
    )

  private final class CompletionManager(allStepsSignal: StrictSignal[List[Step.ID]]) {
    private val completedSteps: Var[List[Step.ID]] = Var(List.empty)

    private val completedStepsSignal: Signal[Set[Step.ID]] =
      completedSteps.signal.map(_.toSet)

    def updateStatus(stepID: Step.ID, isComplete: Boolean): Unit =
      if (isComplete)
        completedSteps.set(allStepsSignal.now().takeWhile(_ != stepID) :+ stepID)
      else
        completedSteps.update(_.takeWhile(_ != stepID))

    def isCompleteSignal(stepID: Step.ID): Signal[Boolean] =
      completedStepsSignal.map(_.contains(stepID))
  }
}
