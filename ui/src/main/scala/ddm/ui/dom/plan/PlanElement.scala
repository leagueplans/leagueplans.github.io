package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.laminar.api.{L, enrichSource}
import ddm.ui.dom.common.{Forester, Modal}
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Effect, Step}

import java.util.UUID

object PlanElement {
  def apply(
    initialPlan: Forest[UUID, Step],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    focusObserver: Observer[UUID],
    showEffect: Effect => L.HtmlElement
  ): (L.Div, Forester[UUID, Step]) = {
    val (modal, modalBus) = Modal()
    val stepUpdates = new EventBus[Forester[UUID, Step] => Unit]
    val forester = Forester[UUID, Step](
      initialPlan,
      _.id,
      toElement(_, _, _, focusedStep, editingEnabled, modalBus, stepUpdates.writer, focusObserver, showEffect)
    )

    val dom =
      L.div(
        modal,
        L.children <-- forester.domSignal,
        stepUpdates.events --> Observer[Forester[UUID, Step] => Unit](_.apply(forester))
      )

    (dom, forester)
  }

  private def toElement(
    stepID: UUID,
    step: Signal[Step],
    subSteps: Signal[L.Children],
    focusedStep: Signal[Option[UUID]],
    editingEnabled: Signal[Boolean],
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    focusObserver: Observer[UUID],
    showEffect: Effect => L.HtmlElement
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
      modalBus,
      stepUpdater,
      focusObserver,
      showEffect
    )
}
