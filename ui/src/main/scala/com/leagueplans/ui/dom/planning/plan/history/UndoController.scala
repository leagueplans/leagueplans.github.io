package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.forest.Forester.HistoryOutcome
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.ToastHub
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, enrichSource, seqToModifier}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt

/** Undoes and redoes changes to a plan's steps, then focuses the affected step and reports what
  * happened */
final class UndoController(
  forester: Forester[Step.ID, Step],
  focusController: FocusController,
  toastPublisher: ToastHub.Publisher
) {
  def undo(): Unit = forester.undo()
  def redo(): Unit = forester.redo()

  val undoLabel: Signal[Option[String]] =
    forester.historyStatus.map(_.nextUndo.map(StepChangeLabel.describe))

  val redoLabel: Signal[Option[String]] =
    forester.historyStatus.map(_.nextRedo.map(StepChangeLabel.describe))

  // Holding down the shortcut undoes many changes in quick succession, so they're reported
  // together once the user stops
  private val unreported = ListBuffer.empty[HistoryOutcome[Step.ID, Step]]

  /** Mount this on the planning page */
  val modifier: L.Modifier[L.Element] =
    List(
      forester.historyOutcomes --> { outcome =>
        outcome match {
          case HistoryOutcome.Undone(entry) => refocus(entry); unreported += outcome
          case HistoryOutcome.Redone(entry) => refocus(entry); unreported += outcome
          case HistoryOutcome.UndoConflict(entry) => reportConflict("undo", entry)
          case HistoryOutcome.RedoConflict(entry) => reportConflict("redo", entry)
        }
      },
      forester.historyOutcomes.debounce(500) --> (_ => reportApplied())
    )

  private def refocus(entry: Entry[Step.ID, Step]): Unit =
    focusController.update((focus, forest) => UndoFocus.target(entry, forest, focus))

  private def reportApplied(): Unit = {
    val outcomes = unreported.toList
    unreported.clear()
    val title = outcomes match {
      case List(HistoryOutcome.Undone(entry)) => Some(s"Undid: ${StepChangeLabel.describe(entry)}")
      case List(HistoryOutcome.Redone(entry)) => Some(s"Redid: ${StepChangeLabel.describe(entry)}")
      case _ =>
        val undos = outcomes.count(_.isInstanceOf[HistoryOutcome.Undone[?, ?]])
        val redos = outcomes.count(_.isInstanceOf[HistoryOutcome.Redone[?, ?]])
        List(
          Option.when(undos > 0)(s"undid ${changes(undos)}"),
          Option.when(redos > 0)(s"redid ${changes(redos)}")
        ).flatten match {
          case Nil => None
          case parts => Some(parts.mkString(", ").capitalize)
        }
    }
    title.foreach(toastPublisher.publish(ToastHub.Type.Info, 4.seconds, _))
  }

  private def changes(count: Int): String =
    if (count == 1) "1 change" else s"$count changes"

  private def reportConflict(action: String, entry: Entry[Step.ID, Step]): Unit =
    toastPublisher.publish(
      ToastHub.Type.Warning,
      10.seconds,
      s"Couldn't $action \"${StepChangeLabel.describe(entry)}\"",
      detail = Some("Those steps have been changed in another tab since.")
    )
}
