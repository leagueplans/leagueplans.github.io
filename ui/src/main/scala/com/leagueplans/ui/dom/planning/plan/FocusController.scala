package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.annotation.tailrec

object FocusController {
  private type Update = (Option[Step.ID], Forest[Step.ID, Step]) => Option[Step.ID]

  def apply(forester: Forester[Step.ID, Step]): (StrictSignal[Option[Step.ID]], FocusController) = {
    val focus = Var(Option.empty[Step.ID]).distinct
    val controller = new FocusController(
      focus.updater[Update]((current, f) =>
        f(current, forester.signal.now())
      )
    )
    (focus.signal, controller)
  }
}

final class FocusController private[FocusController](updater: Observer[FocusController.Update]) {
  def set(step: Step.ID): Unit =
    updater.onNext((_, _) => Some(step))

  def toggle(step: Step.ID): Unit =
    updater.onNext {
      case (Some(`step`), _) => None
      case (_, _) => Some(step)
    }

  def next(ignoreChildren: Boolean): Unit =
    updater.onNext {
      case (None, forest) =>
        forest.roots.headOption
      case (Some(step), forest) =>
        val maybeChild = for {
          children <- forest.toChildren.get(step) if !ignoreChildren
          firstChild <- children.headOption
        } yield firstChild

        maybeChild
          .orElse(nextNonChild(step, forest))
          .orElse(forest.roots.headOption.filterNot(_ == step))
    }

  @tailrec
  private def nextNonChild(step: Step.ID, forest: Forest[Step.ID, Step]): Option[Step.ID] =
    forest.siblings(step).dropWhile(_ != step).drop(1).headOption match {
      case Some(sibling) => Some(sibling)
      case None =>
        forest.toParent.get(step) match {
          case Some(parent) => nextNonChild(parent, forest)
          case None => None
        }
    }

  def previous(ignoreChildren: Boolean): Unit =
    updater.onNext {
      case (None, forest) =>
        forest.roots.lastOption.map(step =>
          if (ignoreChildren) step else lowestDescendant(step, forest)
        )
      case (Some(step), forest) =>
        forest.siblings(step).takeWhile(_ != step).lastOption match {
          case Some(prior) =>
            Some(if (ignoreChildren) prior else lowestDescendant(prior, forest))
          case None =>
            // No earlier sibling. Return the parent, or the final step in the plan
            forest.toParent.get(step).orElse(
              forest.roots.lastOption.map(step =>
                if (ignoreChildren) step else lowestDescendant(step, forest)
              )
            )
        }
    }

  @tailrec
  private def lowestDescendant(step: Step.ID, forest: Forest[Step.ID, Step]): Step.ID =
    forest.toChildren.get(step).flatMap(_.lastOption) match {
      case Some(child) => lowestDescendant(child, forest)
      case None => step
    }

  def firstChild(): Unit =
    updater.onNext {
      case (None, forest) =>
        forest.roots.headOption
      case (Some(step), forest) =>
        forest.toChildren.get(step).flatMap(_.headOption).orElse(Some(step))
    }

  def parent(): Unit =
    updater.onNext {
      case (None, _) => None
      case (Some(step), forest) => forest.toParent.get(step).orElse(Some(step))
    }
}
