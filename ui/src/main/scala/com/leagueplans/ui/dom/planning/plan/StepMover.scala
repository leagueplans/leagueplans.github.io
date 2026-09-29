package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus

/** Moves steps around the plan one position at a time, for keyboard shortcuts */
final class StepMover(forester: Forester[Step.ID, Step]) {
  private val movesBus = EventBus[Step.ID]()

  /** The steps this has moved */
  val moves: EventStream[Step.ID] =
    movesBus.events

  /** Swaps the step with its previous sibling */
  def moveUp(step: Step.ID): Unit =
    swapWithSibling(step, offset = -1)

  /** Swaps the step with its next sibling */
  def moveDown(step: Step.ID): Unit =
    swapWithSibling(step, offset = 1)

  /** Makes the step the last substep of its previous sibling */
  def indent(step: Step.ID): Unit =
    forester.batch(batch =>
      batch.forest.siblings(step).takeWhile(_ != step).lastOption.foreach { previous =>
        batch.move(step, previous)
        movesBus.emit(step)
      }
    )

  /** Makes the step the next sibling of its parent */
  def outdent(step: Step.ID): Unit =
    forester.batch { batch =>
      val forest = batch.forest
      forest.toParent.get(step).foreach { parent =>
        val newOrder = forest.siblings(parent).flatMap(sibling =>
          if (sibling == parent) List(parent, step) else List(sibling)
        )
        forest.toParent.get(parent) match {
          case Some(grandparent) => batch.move(step, grandparent)
          case None => batch.promoteToRoot(step)
        }
        batch.reorder(newOrder)
        movesBus.emit(step)
      }
    }

  private def swapWithSibling(step: Step.ID, offset: Int): Unit =
    forester.batch { batch =>
      val siblings = batch.forest.siblings(step)
      val index = siblings.indexOf(step)
      val target = index + offset
      if (index >= 0 && siblings.indices.contains(target)) {
        batch.reorder(siblings.updated(index, siblings(target)).updated(target, step))
        movesBus.emit(step)
      }
    }
}
