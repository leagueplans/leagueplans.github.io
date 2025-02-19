package com.leagueplans.ui.dom.plan

import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.{StrictSignal, Var}

private[plan] final class CompletionManager(allStepsSignal: StrictSignal[List[Step.ID]]) {
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
