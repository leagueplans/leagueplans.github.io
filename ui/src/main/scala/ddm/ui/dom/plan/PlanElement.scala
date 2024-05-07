package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.forest.Forester
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step

import java.util.UUID

object PlanElement {
  def apply(
    initialPlan: Forest[UUID, Step],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    findStepsWithErrors: Forest[UUID, Step] => Set[UUID],
    stepUpdates: EventBus[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID]
  ): (L.Div, Forester[UUID, Step]) = {
    val allStepsVar = Var(List.empty[UUID])
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
        stepUpdates.events --> Observer[Forester[UUID, Step] => Unit](_.apply(forester)),
        forester.forestSignal.map(_.toList.map(_.id)) --> allStepsVar,
        forester.forestSignal.changes.debounce(1500).map(findStepsWithErrors) --> stepsWithErrorsVar.writer
      )

    (dom, forester)
  }

  private def toElement(
    stepID: UUID,
    step: Signal[Step],
    subSteps: Signal[List[L.Node]],
    focusedStep: Signal[Option[UUID]],
    completionManager: CompletionManager,
    stepsWithErrorsSignal: Signal[Set[UUID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID]
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

  private class CompletionManager(allStepsSignal: StrictSignal[List[UUID]]) {
    private val completedSteps: Var[List[UUID]] = Var(List.empty)

    private val completedStepsSignal: Signal[Set[UUID]] =
      completedSteps.signal.map(_.toSet)

    def updateStatus(stepID: UUID, isComplete: Boolean): Unit =
      if (isComplete)
        completedSteps.set(allStepsSignal.now().takeWhile(_ != stepID) :+ stepID)
      else
        completedSteps.update(_.takeWhile(_ != stepID))

    def isCompleteSignal(stepID: UUID): Signal[Boolean] =
      completedStepsSignal.map(_.contains(stepID))
  }
}
