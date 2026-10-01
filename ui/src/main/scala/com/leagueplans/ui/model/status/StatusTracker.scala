package com.leagueplans.ui.model.status

import com.raquo.airstream.state.{StrictSignal, Var}

import scala.collection.mutable

object StatusTracker {
  val empty: StatusTracker = StatusTracker(mutable.Map.empty)
  
  sealed trait Status

  object Status {
    case object Idle extends Status
    case object Busy extends Status

    /** Problems aren't sealed, so that a component can describe its own. Code that only displays
      * statuses can show any problem through its reason.
      */
    trait Problem extends Status {
      def reason: String
    }

    final case class Failed(reason: String) extends Problem
  }
}

final class StatusTracker(statuses: mutable.Map[String, Var[StatusTracker.Status]]) {
  def get(id: String): Option[StrictSignal[StatusTracker.Status]] =
    statuses.get(id).map(_.signal)
    
  def getWithDefault(
    id: String,
    default: StatusTracker.Status = StatusTracker.Status.Idle
  ): StrictSignal[StatusTracker.Status] =
    statuses.get(id) match {
      case Some(state) => state.signal
      case None =>
        val state = Var(default)
        statuses += (id -> state)
        state.signal
    }

  def set(id: String, status: StatusTracker.Status): Unit =
    statuses.get(id) match {
      case Some(state) => state.set(status)
      case None => statuses += (id -> Var(status))
    }
}
