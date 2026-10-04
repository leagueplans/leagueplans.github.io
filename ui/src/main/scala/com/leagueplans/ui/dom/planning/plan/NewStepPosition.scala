package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step

/** Where a step that's being typed into the plan will go: among the substeps of `parent`, or
  * among the plan's top-level steps if there's no parent, at `index` */
final case class NewStepPosition(parent: Option[Step.ID], index: Int)

object NewStepPosition {
  /** After the last substep of the parent, or at the end of the plan if there's no parent */
  def lastIn(parent: Option[Step.ID], forest: Forest[Step.ID, Step]): NewStepPosition =
    NewStepPosition(parent, childrenOf(parent, forest).size)

  /** Makes it the last substep of the step above it, if there is one */
  def indent(position: NewStepPosition, forest: Forest[Step.ID, Step]): Option[NewStepPosition] = {
    val siblings = childrenOf(position.parent, forest)
    val index = position.index.min(siblings.size)
    Option.when(index > 0)(lastIn(Some(siblings(index - 1)), forest))
  }

  /** Moves it out of its parent, to just after the parent */
  def outdent(position: NewStepPosition, forest: Forest[Step.ID, Step]): Option[NewStepPosition] =
    position.parent.map { parent =>
      val grandparent = forest.toParent.get(parent)
      NewStepPosition(grandparent, childrenOf(grandparent, forest).indexOf(parent) + 1)
    }

  /** Adds the step at the position. If the parent has since been removed, by another tab, the
    * step goes at the end of the plan instead. */
  def insert(step: Step, position: NewStepPosition, batch: Forester.Batch[Step.ID, Step]): Unit = {
    val parent = position.parent.filter(batch.forest.contains)
    val siblings = childrenOf(parent, batch.forest)
    val index = if (parent == position.parent) position.index.min(siblings.size) else siblings.size
    batch.add(step, parent)
    if (index < siblings.size)
      batch.reorder(siblings.patch(index, List(step.id), 0))
  }

  private def childrenOf(parent: Option[Step.ID], forest: Forest[Step.ID, Step]): List[Step.ID] =
    parent.fold(forest.roots)(forest.toChildren.getOrElse(_, List.empty))
}
