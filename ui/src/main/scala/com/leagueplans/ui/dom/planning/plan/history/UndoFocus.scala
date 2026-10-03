package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.plan.Step

/** Chooses the step to focus after an entry has been undone or redone */
object UndoFocus {
  /** In order of preference:
    *  1. the focused step, if the entry changed it
    *  2. the first step in the plan that the entry changed, including steps that it moved within
    *     their parent
    *  3. the first parent whose substeps changed, such as the parent of a step whose addition
    *     was undone
    *  4. the focused step, if it still exists
    */
  def target(
    entry: Entry[Step.ID, Step],
    forest: Forest[Step.ID, Step],
    focus: Option[Step.ID]
  ): Option[Step.ID] = {
    val changed = (entry.touched.nodes ++ reordered(entry)).filter(forest.contains)
    val parents = entry.touched.lists.flatten.filter(forest.contains)

    focus.filter(changed.contains)
      .orElse(first(forest, changed))
      .orElse(first(forest, parents))
      .orElse(focus.filter(forest.contains))
  }

  /** Steps whose order relative to their siblings changed. Siblings that were added or removed
    * don't count, so the steps after a deleted step don't count as reordered. */
  private def reordered(entry: Entry[Step.ID, Step]): Set[Step.ID] =
    entry.touched.lists.flatMap { key =>
      val before = entry.before.lists.get(key).flatten.toList.flatten
      val after = entry.after.lists.get(key).flatten.toList.flatten
      val common = before.toSet.intersect(after.toSet)
      before.filter(common.contains).zip(after.filter(common.contains)).collect {
        case (b, a) if b != a => b
      }
    }

  private def first(forest: Forest[Step.ID, Step], steps: Set[Step.ID]): Option[Step.ID] =
    Option.when(steps.nonEmpty)(forest.toLazyList.find(steps.contains)).flatten
}
