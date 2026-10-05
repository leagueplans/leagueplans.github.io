package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.ToastHub

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalajs.js

object UndoToasts {
  /** How long toasts for small changes, such as moving items, stay up */
  val brief: FiniteDuration = 4.seconds
}

/** Reports a change to the plan in a toast with an Undo button. The button only undoes the
  * change while it's still the latest one. Otherwise it would undo something newer.
  */
final class UndoToasts(forester: Forester[Step.ID, Step], toastPublisher: ToastHub.Publisher) {
  /** Call straight after making the change.
    *
    * @param change what the warning calls the change if it can't be undone, such as "the deletion"
    * @param duration how long the toast stays up. Small changes, such as moving items, can use
    *                 [[UndoToasts.brief]], so that toasts don't pile up while they're being made.
    */
  def report(
    title: String,
    detail: Option[String] = None,
    change: String = "the change",
    duration: FiniteDuration = 8.seconds
  ): Unit =
    // Inside an event handler, the change only happens once the handler has finished
    js.timers.setTimeout(0) {
      val entry = forester.historyStatus.now().nextUndo
      toastPublisher.publish(
        ToastHub.Type.Info,
        duration,
        title,
        detail,
        action = Some(ToastHub.Action("Undo", () => undo(entry, change))),
        // Undoing reports in a toast with the same key, which replaces this one
        key = Some("plan-history")
      )
    }: Unit

  private def undo(change: Option[Entry[Step.ID, Step]], description: String): Unit =
    if (change.exists(entry => forester.historyStatus.now().nextUndo.exists(_ eq entry)))
      forester.undo()
    else
      toastPublisher.publish(
        ToastHub.Type.Warning,
        6.seconds,
        s"Couldn't undo $description from here",
        detail = Some("You've made other changes since. Undo them first, with Ctrl+Z.")
      )
}
