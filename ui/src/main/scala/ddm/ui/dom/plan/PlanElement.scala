package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.dom.common.Forester
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step

import java.util.UUID

object PlanElement {
  def apply(
    initialPlan: Forest[UUID, Step],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    focusObserver: Observer[UUID]
  ): (L.Div, Forester[UUID, Step]) = {
    val stepUpdates = new EventBus[Step]
    val forester = Forester[UUID, Step](
      initialPlan,
      _.id,
      toElement(_, _, focusedStep, editingEnabled, stepUpdates.writer, focusObserver)
    )

    val dom =
      L.div(
        L.children <-- forester.domSignal,
        stepUpdates.events --> Observer(forester.update)
      )

    (dom, forester)
  }

  private def toElement(
    step: Signal[Step],
    subSteps: Signal[L.Children],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    stepUpdater: Observer[Step],
    focusObserver: Observer[UUID]
  ): L.Child =
    StepElement(
      step,
      subSteps,
      Signal.combine(step, focusedStep).map {
        case (s, Some(focus)) if s.id == focus => StepElement.Theme.Focused
        case _ => StepElement.Theme.NotFocused
      },
      editingEnabled,
      stepUpdater,
      focusObserver
    )
}
