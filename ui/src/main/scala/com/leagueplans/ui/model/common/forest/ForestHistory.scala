package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update

object ForestHistory {
  /** A recorded change. `before` and `after` hold the state of the touched parts of the forest
    * either side of the change, and nothing else. */
  final case class Entry[ID, T](touched: Touched[ID], before: Slice[ID, T], after: Slice[ID, T]) {
    /** Roughly the memory the entry holds, in units of about 20 bytes. Measured in Node, each
      * touched node or list costs about 20 units, and each ID in a captured list about 1. The
      * entry's data isn't counted, as it's usually shared with the forest. */
    lazy val weight: Int =
      20 * (touched.nodes.size + touched.lists.size) +
        (before.lists.valuesIterator ++ after.lists.valuesIterator).map(_.fold(0)(_.size)).sum
  }

  enum Result[ID, T] {
    case NothingToDo()
    /** The updates take the forest to the entry's other side. `history` is the new history. */
    case Applied(entry: Entry[ID, T], updates: List[Update[ID, T]], history: ForestHistory[ID, T])
    /** Part of the forest that the entry touched has changed since, so the entry no longer
      * applies. It has been discarded from `history`. */
    case Conflict(entry: Entry[ID, T], history: ForestHistory[ID, T])
  }

  /** @param maxEntries the most entries to keep
    * @param maxWeight the most total [[Entry.weight]] to keep. The most recent entry is always
    *                  kept, however heavy it is.
    */
  def empty[ID, T](maxEntries: Int, maxWeight: Int): ForestHistory[ID, T] =
    ForestHistory(List.empty, List.empty, maxEntries, maxWeight)
}

/** Undo and redo stacks for a forest.
  *
  * Undo and redo only apply an entry when the parts of the forest it touched are exactly as the
  * entry left them. Changes made elsewhere in the forest, for example by another tab, are kept.
  *
  * @param undoStack most recent first
  * @param redoStack most recently undone first
  */
final case class ForestHistory[ID, T](
  undoStack: List[ForestHistory.Entry[ID, T]],
  redoStack: List[ForestHistory.Entry[ID, T]],
  maxEntries: Int,
  maxWeight: Int
) {
  import ForestHistory.{Entry, Result}

  /** Records a change, unless it made no difference. Recording a change clears the redo stack,
    * and drops the oldest entries that exceed the limits.
    *
    * @param candidates the parts of the forest the change may have touched
    */
  def record(before: Forest[ID, T], after: Forest[ID, T], candidates: Touched[ID]): ForestHistory[ID, T] = {
    val touched = Touched.changed(before, after, candidates)
    if (touched.isEmpty)
      this
    else {
      val entry = Entry(touched, Slice.capture(before, touched), Slice.capture(after, touched))
      copy(undoStack = trim(entry +: undoStack), redoStack = List.empty)
    }
  }

  private def trim(entries: List[Entry[ID, T]]): List[Entry[ID, T]] = {
    val cumulativeWeights = entries.iterator.scanLeft(0L)(_ + _.weight).drop(1)
    val withinBudget = cumulativeWeights.takeWhile(_ <= maxWeight).size
    entries.take(withinBudget.max(1).min(maxEntries))
  }

  def undo(current: Forest[ID, T]): Result[ID, T] =
    undoStack match {
      case Nil => Result.NothingToDo()
      case entry :: remaining =>
        travel(current, entry, from = entry.after, to = entry.before) match {
          case Some(updates) => Result.Applied(entry, updates, copy(undoStack = remaining, redoStack = entry +: redoStack))
          case None => Result.Conflict(entry, copy(undoStack = remaining))
        }
    }

  def redo(current: Forest[ID, T]): Result[ID, T] =
    redoStack match {
      case Nil => Result.NothingToDo()
      case entry :: remaining =>
        travel(current, entry, from = entry.before, to = entry.after) match {
          case Some(updates) => Result.Applied(entry, updates, copy(undoStack = entry +: undoStack, redoStack = remaining))
          case None => Result.Conflict(entry, copy(redoStack = remaining))
        }
    }

  /** The updates that take the touched parts of the current forest from one slice to the other,
    * or `None` if the current forest doesn't match the starting slice */
  private def travel(
    current: Forest[ID, T],
    entry: Entry[ID, T],
    from: Slice[ID, T],
    to: Slice[ID, T]
  ): Option[List[Update[ID, T]]] =
    for {
      _ <- Option.when(Slice.capture(current, entry.touched) == from)(())
      target = Slice.patch(current, to)
      // The checks below should always pass when the current forest matches the starting slice
      if isConsistent(target)
      updates <- ForestDiff.diff(current, target, entry.touched).toOption
    } yield updates

  private def isConsistent(forest: Forest[ID, T]): Boolean =
    Forest.validated(forest.nodes, forest.toChildren, forest.roots).contains(forest)
}
