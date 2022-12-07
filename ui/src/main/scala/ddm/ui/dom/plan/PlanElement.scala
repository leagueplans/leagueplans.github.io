package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step

import java.util.UUID

object PlanElement {
  def apply(
    initialPlan: Forest[UUID, Step],
    focusedStep: Signal[Option[UUID]],
    focusObserver: Observer[UUID]
  ): (L.Div, Forester[UUID, Step]) = {
    val forester = Forester[UUID, Step](initialPlan, _.id, toElement(_, focusedStep, _, focusObserver))
    (L.div(L.children <-- forester.domSignal), forester)
  }

  private def toElement(
    step: Signal[Step],
    focusedStep: Signal[Option[UUID]],
    subSteps: Signal[L.Children],
    focusObserver: Observer[UUID]
  ): L.Child =
    StepElement(
      step,
      Signal.combine(step, focusedStep).map {
        case (s, Some(focus)) if s.id == focus => StepElement.Theme.Focused
        case _ => StepElement.Theme.NotFocused
      },
      focusObserver,
      subSteps
    )
}
