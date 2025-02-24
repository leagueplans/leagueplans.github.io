package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.enrichSource
import com.raquo.laminar.modifiers.Binder

object CompletedStep {
  private type Update = Forest[Step.ID, Step] => Set[Step.ID]

  //TODO This needs reworking. It doesn't handle step reordering.
  // Probably the best thing to do is just move completion status to a
  // saved property on the step itself. There's not a clear way to
  // reconcile moving completed steps, so just let the user do it.
  final class Controller private[CompletedStep](
    val signal: StrictSignal[Set[Step.ID]],
    updateObserver: Observer[Update]
  ) {
    def signalFor(step: Step.ID): Signal[Boolean] =
      signal.map(_.contains(step)).distinct
    
    def setStatus(step: Step.ID, isComplete: Boolean): Unit =
      updateObserver.onNext(forest =>
        forest.toList.takeWhile(_.id != step).map(_.id).toSet ++
          Option.when(isComplete)(step)
      )
  }

  def apply(forestSignal: Signal[Forest[Step.ID, Step]]): (Binder.Base, Controller) = {
    val completed = Var(Set.empty[Step.ID])
    val updateBus = EventBus[Update]()
    val binding = toBinding(updateBus.events, forestSignal, toUpdater(completed))
    val controller = Controller(completed.signal, updateBus.writer)
    (binding, controller)
  }

  private def toBinding(
    events: EventStream[Update],
    forestSignal: Signal[Forest[Step.ID, Step]],
    observer: Observer[(Update, Forest[Step.ID, Step])]
  ): Binder.Base =
    events.withCurrentValueOf(forestSignal) --> observer

  private def toUpdater(completedVar: Var[Set[Step.ID]]): Observer[(Update, Forest[Step.ID, Step])] =
    Observer((f, forest) => completedVar.set(f(forest)))
}
