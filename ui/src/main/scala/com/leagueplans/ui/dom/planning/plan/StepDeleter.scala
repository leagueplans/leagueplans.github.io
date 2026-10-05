package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.Step

/** Deletes a step, with its substeps, straight away. A toast says what was deleted and offers
  * to undo it. */
final class StepDeleter(
  forester: Forester[Step.ID, Step],
  focusController: FocusController,
  undoToasts: UndoToasts
) {
  def delete(step: Step.ID): Unit = {
    val forest = forester.signal.now()
    forest.nodes.get(step).foreach { deleted =>
      val substeps = forest.subtree(step).nodes.size - 1
      focusController.moveOutOf(step)
      forester.remove(step)
      undoToasts.report(
        s"Deleted “${shorten(deleted.description)}”",
        detail = Option.when(substeps > 0)(if (substeps == 1) "and its substep" else s"and its $substeps substeps"),
        change = "the deletion"
      )
    }
  }

  private def shorten(description: String): String = {
    val firstLine = description.linesIterator.nextOption().getOrElse("")
    if (firstLine.length <= 60 && firstLine == description) firstLine else s"${firstLine.take(60).trim}…"
  }
}
