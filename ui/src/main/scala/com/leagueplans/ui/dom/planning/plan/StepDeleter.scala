package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.ToastHub

import scala.concurrent.duration.DurationInt
import scala.scalajs.js

/** Deletes a step, with its substeps, straight away. A toast says what was deleted and offers
  * to undo it. */
final class StepDeleter(
  forester: Forester[Step.ID, Step],
  focusController: FocusController,
  toastPublisher: ToastHub.Publisher
) {
  def delete(step: Step.ID): Unit = {
    val forest = forester.signal.now()
    forest.nodes.get(step).foreach { deleted =>
      val substeps = forest.subtree(step).nodes.size - 1
      focusController.moveOutOf(step)
      forester.remove(step)
      // Inside an event handler, the deletion only happens once the handler has finished
      js.timers.setTimeout(0)(report(deleted, substeps)): Unit
    }
  }

  private def report(deleted: Step, substeps: Int): Unit = {
    val entry = forester.historyStatus.now().nextUndo
    toastPublisher.publish(
      ToastHub.Type.Info,
      8.seconds,
      s"Deleted “${shorten(deleted.description)}”",
      detail = Option.when(substeps > 0)(if (substeps == 1) "and its substep" else s"and its $substeps substeps"),
      action = Some(ToastHub.Action("Undo", () => undo(entry))),
      // Undoing reports in a toast with the same key, which replaces this one
      key = Some("plan-history")
    )
  }

  /** Only undoes the deletion while it's still the latest change. Otherwise the button would
    * undo a later change instead. */
  private def undo(deletion: Option[Entry[Step.ID, Step]]): Unit =
    if (deletion.exists(entry => forester.historyStatus.now().nextUndo.exists(_ eq entry)))
      forester.undo()
    else
      toastPublisher.publish(
        ToastHub.Type.Warning,
        6.seconds,
        "Couldn't undo the deletion from here",
        detail = Some("You've made other changes since. Undo them first, with Ctrl+Z.")
      )

  private def shorten(description: String): String = {
    val firstLine = description.linesIterator.nextOption().getOrElse("")
    if (firstLine.length <= 60 && firstLine == description) firstLine else s"${firstLine.take(60).trim}…"
  }
}
