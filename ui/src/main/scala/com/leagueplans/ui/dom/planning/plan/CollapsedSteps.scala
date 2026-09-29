package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.model.PlanID
import org.scalajs.dom.window.localStorage

import scala.util.control.NonFatal

object CollapsedSteps {
  private def key(planID: PlanID): String =
    s"collapsed-steps-$planID"

  /** Loads the collapsed steps for a plan, forgetting any that are no longer in the plan */
  def apply(planID: PlanID, steps: Set[Step.ID]): CollapsedSteps = {
    val stored =
      attempt(Option(localStorage.getItem(key(planID)))).flatten match {
        case Some(ids) => ids.split(',').toSet.filter(_.nonEmpty).map(Step.ID.fromString)
        case None => Set.empty[Step.ID]
      }
    val collapsed = new CollapsedSteps(key(planID), stored.intersect(steps))
    if (collapsed.all != stored) collapsed.save()
    collapsed
  }

  /** Forgets the collapsed steps for a plan, such as when the plan is deleted */
  def forget(planID: PlanID): Unit =
    attempt(localStorage.removeItem(key(planID))): Unit

  // Browser storage can be unavailable, for example in private windows or when site data is
  // blocked. Remembering collapsed steps is only a convenience, so failures are ignored.
  private def attempt[T](f: => T): Option[T] =
    try Some(f) catch { case NonFatal(_) => None }
}

/** Remembers which steps the user has collapsed in a plan, in the browser's local storage, so
  * that they stay collapsed when the plan is next opened in this browser */
final class CollapsedSteps private (key: String, initial: Set[Step.ID]) {
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
    CollapsedSteps.attempt(
      if (collapsed.isEmpty) localStorage.removeItem(key)
      else localStorage.setItem(key, collapsed.mkString(","))
    ): Unit
}
