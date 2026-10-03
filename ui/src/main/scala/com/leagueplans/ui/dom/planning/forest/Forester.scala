package com.leagueplans.ui.dom.planning.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.ForestHistory.{Entry, Result}
import com.leagueplans.ui.model.common.forest.{Forest, ForestHistory, ForestInterpreter, ForestResolver, Touched}
import com.leagueplans.uicommon.utils.HasID
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.collection.mutable.ListBuffer

object Forester {
  def apply[ID, T](
    forest: Forest[ID, T],
    externalObserver: Observer[List[Forest.Update[ID, T]]]
  )(using HasID.Aux[T, ID]): Forester[ID, T] =
    new Forester(
      Var(forest).distinct,
      externalObserver,
      // See .claude/plans/04-undo-redo.md for how these were measured
      ForestHistory.empty(maxEntries = 200, maxWeight = 1_000_000)
    )

  enum HistoryOutcome[ID, T] {
    case Undone(entry: Entry[ID, T])
    case Redone(entry: Entry[ID, T])
    /** The entry was discarded, because part of the forest it touched has changed since */
    case UndoConflict(entry: Entry[ID, T])
    /** The entry was discarded, because part of the forest it touched has changed since */
    case RedoConflict(entry: Entry[ID, T])
  }

  final case class HistoryStatus[ID, T](nextUndo: Option[Entry[ID, T]], nextRedo: Option[Entry[ID, T]])
  /** A group of operations whose updates are emitted together. Each operation applies to the
    * forest as the operations before it left it. */
  final class Batch[ID, T] private[Forester](initial: Forest[ID, T])(using HasID.Aux[T, ID]) {
    private var current = initial
    private val collected = ListBuffer.empty[Update[ID, T]]

    /** The forest, including the effects of the operations so far */
    def forest: Forest[ID, T] =
      current

    def add(data: T): Unit =
      run(_.add(data))

    def add(child: T, parent: ID): Unit =
      run(_.add(child, parent))

    def add(child: T, maybeParent: Option[ID]): Unit =
      run(_.addOption(child, maybeParent))

    def move(child: ID, newParent: ID): Unit =
      run(_.move(child, Some(newParent)))

    def promoteToRoot(child: ID): Unit =
      run(_.move(child, None))

    def remove(id: ID): Unit =
      run(_.remove(id))

    def update(id: ID, f: T => T): Unit =
      run(_.update(id, f))

    def update(data: T): Unit =
      run(_.update(data))

    def reorder(newOrder: List[ID]): Unit =
      run(_.reorder(newOrder))

    private[Forester] def updates: List[Update[ID, T]] =
      collected.toList

    private def run(f: ForestInterpreter[ID, T] => List[Update[ID, T]]): Unit = {
      val updates = f(ForestInterpreter(current))
      current = ForestResolver.resolve(current, updates)
      collected ++= updates
    }
  }
}

/** Optimises updates to the forest, and records them so that they can be undone.
  *
  * The updates produced by each operation, or by each [[batch]] of operations, are emitted
  * together, as a single non-empty batch. Each batch is one entry in the undo history, so a
  * gesture that makes several changes should make them in one batch.
  *
  * All local edits to the forest must go through a forester, or they can't be undone. */
final class Forester[ID, T](
  forestState: Var[Forest[ID, T]],
  externalObserver: Observer[List[Forest.Update[ID, T]]],
  initialHistory: ForestHistory[ID, T]
)(using HasID.Aux[T, ID]) {
  import Forester.{HistoryOutcome, HistoryStatus}

  val signal: StrictSignal[Forest[ID, T]] =
    forestState.signal

  private val updateBus = EventBus[List[Update[ID, T]]]()
  /** A stream of _all_ batches handled by this forester, including those that were injected */
  val updates: EventStream[List[Update[ID, T]]] = updateBus.events

  private var history = initialHistory
  private val historyStatusState = Var(HistoryStatus[ID, T](None, None))
  val historyStatus: StrictSignal[HistoryStatus[ID, T]] = historyStatusState.signal

  private val historyOutcomeBus = EventBus[HistoryOutcome[ID, T]]()
  /** Emitted after the forest has been updated */
  val historyOutcomes: EventStream[HistoryOutcome[ID, T]] = historyOutcomeBus.events

  def add(data: T): Unit =
    batch(_.add(data))

  def add(child: T, parent: ID): Unit =
    batch(_.add(child, parent))

  def add(child: T, maybeParent: Option[ID]): Unit =
    batch(_.add(child, maybeParent))

  def move(child: ID, newParent: ID): Unit =
    batch(_.move(child, newParent))

  def promoteToRoot(child: ID): Unit =
    batch(_.promoteToRoot(child))

  def remove(id: ID): Unit =
    batch(_.remove(id))

  def update(id: ID, f: T => T): Unit =
    batch(_.update(id, f))

  def update(data: T): Unit =
    batch(_.update(data))

  def reorder(newOrder: List[ID]): Unit =
    batch(_.reorder(newOrder))

  /** Applies the operations in order, and emits all of their updates as one batch.
    *
    * Everything happens inside the Var's update function. When called from within an Airstream
    * transaction, the function is deferred until the transaction ends, and runs against the
    * result of any earlier batches. Operations that depend on the forest should therefore read
    * it from [[Forester.Batch.forest]], rather than from [[signal]]. */
  def batch(operations: Forester.Batch[ID, T] => Unit): Unit =
    forestState.update { forest =>
      val batch = Forester.Batch(forest)
      operations(batch)
      val updates = batch.updates
      if (updates.nonEmpty) {
        emit(updates)
        setHistory(history.record(forest, batch.forest, Touched.from(updates)))
      }
      batch.forest
    }

  /** Undoes the most recent recorded batch. Like [[batch]], the undo is deferred when called
    * from within an Airstream transaction. */
  def undo(): Unit =
    travel(_.undo(_), HistoryOutcome.Undone(_), HistoryOutcome.UndoConflict(_))

  /** Redoes the most recently undone batch. Like [[batch]], the redo is deferred when called
    * from within an Airstream transaction. */
  def redo(): Unit =
    travel(_.redo(_), HistoryOutcome.Redone(_), HistoryOutcome.RedoConflict(_))

  private def travel(
    step: (ForestHistory[ID, T], Forest[ID, T]) => Result[ID, T],
    onApplied: Entry[ID, T] => HistoryOutcome[ID, T],
    onConflict: Entry[ID, T] => HistoryOutcome[ID, T]
  ): Unit =
    forestState.update { forest =>
      step(history, forest) match {
        case Result.NothingToDo() =>
          forest

        case Result.Conflict(entry, updatedHistory) =>
          setHistory(updatedHistory)
          historyOutcomeBus.emit(onConflict(entry))
          forest

        case Result.Applied(entry, updates, updatedHistory) =>
          // Saved like any other batch, but not recorded
          emit(updates)
          setHistory(updatedHistory)
          historyOutcomeBus.emit(onApplied(entry))
          ForestResolver.resolve(forest, updates)
      }
    }

  private def emit(updates: List[Update[ID, T]]): Unit = {
    externalObserver.onNext(updates)
    updateBus.emit(updates)
  }

  private def setHistory(updated: ForestHistory[ID, T]): Unit = {
    history = updated
    historyStatusState.set(HistoryStatus(updated.undoStack.headOption, updated.redoStack.headOption))
  }

  /** Intended for batches that should not be propagated to an external observer. They aren't
    * recorded in the undo history either. */
  def inject(updates: List[Update[ID, T]]): Unit =
    forestState.update { forest =>
      val updated = ForestResolver.resolve(forest, updates)
      if (updates.nonEmpty)
        updateBus.emit(updates)
      updated
    }
}
