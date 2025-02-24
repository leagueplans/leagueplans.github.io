package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.enrichSource
import com.raquo.laminar.modifiers.Binder

import scala.annotation.tailrec

object FocusedStep {
  private type Update = (Option[Step.ID], Forest[Step.ID, Step]) => Option[Step.ID]

  final class Controller private[FocusedStep](
    val signal: StrictSignal[Option[Step.ID]],
    updateObserver: Observer[Update]
  ) {
    def signalFor(step: Step.ID): Signal[Boolean] =
      signal.map(_.contains(step))
    
    def set(step: Step.ID): Unit =
      updateObserver.onNext((_, _) => Some(step))

    def toggle(step: Step.ID): Unit =
      updateObserver.onNext {
        case (Some(`step`), _) => None
        case (_, _) => Some(step)
      }

    def next(ignoreChildren: Boolean): Unit =
      updateObserver.onNext {
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
    private def nextNonChild(step: Step.ID, forest: Forest[Step.ID, Step]): Option[Step.ID] = {
      val maybeParent = forest.toParent.get(step)
      val siblings = maybeParent match {
        case None => forest.roots
        case Some(parent) => forest.toChildren.get(parent).toList.flatten
      }

      siblings.dropWhile(_ != step).drop(1).headOption match {
        case Some(sibling) => Some(sibling)
        case None =>
          maybeParent match {
            case Some(parent) => nextNonChild(parent, forest)
            case None => None
          }
      }
    }

    def previous(ignoreChildren: Boolean): Unit =
      updateObserver.onNext {
        case (None, forest) =>
          forest.roots.lastOption.map(step =>
            if (ignoreChildren) step else lowestDescendant(step, forest)
          )

        case (Some(step), forest) =>
          val maybeParent = forest.toParent.get(step)
          val siblings = maybeParent match {
            case None => forest.roots
            case Some(parent) => forest.toChildren.get(parent).toList.flatten
          }

          siblings.takeWhile(_ != step).lastOption match {
            case Some(prior) =>
              Some(if (ignoreChildren) prior else lowestDescendant(prior, forest))

            case None =>
              maybeParent.orElse(
                // No siblings + no parent implies this is the only root
                Option.when(!ignoreChildren)(lowestDescendant(step, forest))
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
      updateObserver.onNext {
        case (None, forest) =>
          forest.roots.headOption
        case (Some(step), forest) =>
          forest.toChildren.get(step).flatMap(_.headOption).orElse(Some(step))
      }

    def parent(): Unit =
      updateObserver.onNext {
        case (None, forest) => None
        case (Some(step), forest) => forest.toParent.get(step).orElse(Some(step))
      }
  }

  def apply(): (Signal[Forest[Step.ID, Step]] => Binder.Base, Controller) = {
    val focus = Var(Option.empty[Step.ID]).distinct
    val updateBus = EventBus[Update]()
    val bindingFn = toBindingFn(updateBus.events, toUpdater(focus))
    val controller = Controller(focus.signal, updateBus.writer)
    (bindingFn, controller)
  }
  
  private def toBindingFn(
    events: EventStream[Update],
    observer: Observer[(Update, Forest[Step.ID, Step])]
  )(forestSignal: Signal[Forest[Step.ID, Step]]): Binder.Base =
    events.withCurrentValueOf(forestSignal) --> observer

  private def toUpdater(focusVar: Var[Option[Step.ID]]): Observer[(Update, Forest[Step.ID, Step])] =
    focusVar.updater[(Update, Forest[Step.ID, Step])] {
      case (current, (f, forest)) => f(current, forest)
    }
}
