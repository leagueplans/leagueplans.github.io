package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.local.PlanLocalStorage

object CollapsedSteps {
  /** Loads the collapsed steps for a plan, forgetting any that are no longer in the plan */
  def apply(storage: PlanLocalStorage, steps: Set[Step.ID]): CollapsedSteps = {
    val stored =
      storage.get(PlanLocalStorage.Key.CollapsedSteps) match {
        case Some(ids) => ids.split(',').toSet.filter(_.nonEmpty).map(Step.ID.fromString)
        case None => Set.empty[Step.ID]
      }
    val collapsed = new CollapsedSteps(storage, stored.intersect(steps))
    if (collapsed.all != stored) collapsed.save()
    collapsed
  }
}

/** Remembers which steps the user has collapsed in a plan, so that they stay collapsed when the
  * plan is next opened in this browser */
final class CollapsedSteps private (storage: PlanLocalStorage, initial: Set[Step.ID]) {
  private var collapsed = initial

  private def all: Set[Step.ID] =
    collapsed

  def isCollapsed(step: Step.ID): Boolean =
    collapsed.contains(step)

  def set(step: Step.ID, isCollapsed: Boolean): Unit = {
    val updated = if (isCollapsed) collapsed + step else collapsed - step
    if (updated != collapsed) {
      collapsed = updated
      save()
    }
  }

  private def save(): Unit =
    if (collapsed.isEmpty) storage.remove(PlanLocalStorage.Key.CollapsedSteps)
    else storage.set(PlanLocalStorage.Key.CollapsedSteps, collapsed.mkString(","))
}
