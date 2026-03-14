package com.leagueplans.ui.dom.planning.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.{Forest, ForestInterpreter, ForestResolver}
import com.leagueplans.ui.utils.HasID
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}

object Forester {
  def apply[ID, T](
    forest: Forest[ID, T],
    externalObserver: Observer[Forest.Update[ID, T]]
  )(using HasID.Aux[T, ID]): Forester[ID, T] =
    new Forester(Var(forest).distinct, externalObserver)
}

/** Optimises updates to the forest */
final class Forester[ID, T](
  forestState: Var[Forest[ID, T]],
  externalObserver: Observer[Forest.Update[ID, T]]
)(using HasID.Aux[T, ID]) {
  val signal: StrictSignal[Forest[ID, T]] =
    forestState.signal

  private val updateBus = EventBus[Update[ID, T]]()
  /** A stream of _all_ events handled by this forester, including those that were injected */
  val updates: EventStream[Update[ID, T]] = updateBus.events

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

  private def run(f: ForestInterpreter[ID, T] => List[Update[ID, T]]): Unit =
    forestState.update { forest =>
      val updates = f(ForestInterpreter(forest))
      updates.foreach { update =>
        externalObserver.onNext(update)
        updateBus.emit(update)
      }
      ForestResolver.resolve(forest, updates)
    }
  
  /** Intended for events that should not be propagated to an external observer */
  def inject(update: Update[ID, T]): Unit =
    forestState.update { forest =>
      updateBus.emit(update)
      ForestResolver.resolve(forest, update)
    }
}
