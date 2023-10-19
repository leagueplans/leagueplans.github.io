package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.dom.common.{ContextMenu, Forester}
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step

import java.util.UUID

object PlanElement {
  def apply(
    initialPlan: Forest[UUID, Step],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdates: EventBus[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID]
  ): (L.Div, Forester[UUID, Step]) = {
    val forester = Forester[UUID, Step](
      initialPlan,
      _.id,
      toElement(_, _, _, focusedStep, editingEnabled, contextMenuController, stepUpdates.writer, focusObserver)
    )

    val dom =
      L.div(
        L.children <-- forester.domSignal,
        stepUpdates.events --> Observer[Forester[UUID, Step] => Unit](_.apply(forester))
      )

    (dom, forester)
  }

  private def toElement(
    stepID: UUID,
    step: Signal[Step],
    subSteps: Signal[List[L.Node]],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    contextMenuController: ContextMenu.Controller,
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID]
  ): L.HtmlElement =
    StepElement(
      stepID,
      step,
      subSteps,
      Signal.combine(step, focusedStep).map {
        case (s, Some(focus)) if s.id == focus => StepElement.Theme.Focused
        case _ => StepElement.Theme.NotFocused
      },
      editingEnabled,
      contextMenuController,
      stepUpdater,
      focusObserver
    )
}
