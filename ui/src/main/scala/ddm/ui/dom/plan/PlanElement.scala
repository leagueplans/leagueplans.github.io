package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.forest.Forester
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step


object PlanElement {
  def apply(
    initialPlan: Forest[Step.ID, Step],
    focusedStep: Signal[Option[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    findStepsWithErrors: Forest[Step.ID, Step] => Set[Step.ID],
    stepUpdates: EventBus[Forester[Step.ID, Step] => Unit],
    focusObserver: Observer[Step.ID]
  ): (L.Div, Forester[Step.ID, Step]) = {
    val allStepsVar = Var(List.empty[Step.ID])
    val completionManager = CompletionManager(allStepsVar.signal)
    val stepsWithErrorsVar = Var(findStepsWithErrors(initialPlan))

    val forester = Forester[Step](
      initialPlan,
      toElement(
        _,
        _,
        _,
        focusedStep,
        completionManager,
        stepsWithErrorsVar.signal,
        editingEnabled,
        contextMenuController,
        stepUpdates.writer,
        focusObserver
      )
    )

    val dom =
      L.div(
        L.children <-- forester.domSignal,
        stepUpdates.events --> (_.apply(forester)),
        forester.forestSignal.map(_.toList.map(_.id)) --> allStepsVar,
        forester.forestSignal.changes.debounce(1500).map(findStepsWithErrors) --> stepsWithErrorsVar
      )

    (dom, forester)
  }

  private def toElement(
    stepID: Step.ID,
    step: Signal[Step],
    subSteps: Signal[List[L.Node]],
    focusedStep: Signal[Option[Step.ID]],
    completionManager: CompletionManager,
    stepsWithErrorsSignal: Signal[Set[Step.ID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    focusObserver: Observer[Step.ID]
  ): L.HtmlElement =
    StepElement(
      stepID,
      step,
      subSteps,
      isFocused = Signal.combine(step, focusedStep).map {
        case (s, Some(focus)) => s.id == focus
        case _ => false
      },
      isCompleteSignal = completionManager.isCompleteSignal(stepID),
      hasErrorsSignal = stepsWithErrorsSignal.map(_.contains(stepID)),
      editingEnabled,
      contextMenuController,
      stepUpdater,
      completionStatusObserver = Observer[Boolean](completionManager.updateStatus(stepID, _)),
      focusObserver
    )

  private class CompletionManager(allStepsSignal: StrictSignal[List[Step.ID]]) {
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
