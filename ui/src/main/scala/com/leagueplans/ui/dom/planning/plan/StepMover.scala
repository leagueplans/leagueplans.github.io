package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus

/** Moves steps around the plan one position at a time, for keyboard shortcuts.
  *
  * Calls made from within an Airstream transaction (like an event handler) defer each forest
  * update until the transaction ends, so every target order is derived from the forest as it
  * was before the move. */
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
  def indent(step: Step.ID): Unit = {
    val forest = forester.signal.now()
    forest.siblings(step).takeWhile(_ != step).lastOption.foreach { previous =>
      forester.move(step, previous)
      movesBus.emit(step)
    }
  }

  /** Makes the step the next sibling of its parent */
  def outdent(step: Step.ID): Unit = {
    val forest = forester.signal.now()
    forest.toParent.get(step).foreach { parent =>
      val newOrder = forest.siblings(parent).flatMap(sibling =>
        if (sibling == parent) List(parent, step) else List(sibling)
      )
      forest.toParent.get(parent) match {
        case Some(grandparent) => forester.move(step, grandparent)
        case None => forester.promoteToRoot(step)
      }
      forester.reorder(newOrder)
      movesBus.emit(step)
    }
  }

  private def swapWithSibling(step: Step.ID, offset: Int): Unit = {
    val siblings = forester.signal.now().siblings(step)
    val index = siblings.indexOf(step)
    val target = index + offset
    if (index >= 0 && siblings.indices.contains(target)) {
      forester.reorder(siblings.updated(index, siblings(target)).updated(target, step))
      movesBus.emit(step)
    }
  }
}
