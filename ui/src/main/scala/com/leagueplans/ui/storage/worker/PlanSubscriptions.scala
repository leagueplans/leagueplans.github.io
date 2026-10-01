package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.LamportTimestamp.increment
import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanID}
import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.wrappers.locks.Locks
import com.raquo.airstream.core.EventStream

import scala.collection.mutable
import scala.util.{Failure, Success}

/** The ports subscribed to each plan, along with the plan's Lamport timestamp and its lock.
  *
  * Each deployed version of the app runs its own coordinator, because the coordinator's script
  * has a different name in each build. Without locks, tabs on different versions could edit the
  * same plan without seeing each other's changes. So a coordinator holds a plan's lock while it
  * has subscribers for the plan. When two versions want the same plan, the one that asked most
  * recently wins, and the other's subscribers are dropped.
  *
  * To subscribe a port, take the plan's [[lock]], then [[register]] the port. Deregistering the
  * last port releases the lock. */
final class PlanSubscriptions[Port](locks: Locks) {
  private val subscriptions = mutable.Map.empty[PlanID, (LamportTimestamp, Set[Port])]
  private val held = mutable.Map.empty[PlanID, Locks.Held]

  /** Takes the plan's lock, if this coordinator doesn't already hold it. Emits the reason if the
    * lock can't be taken.
    *
    * If another coordinator later takes the lock, the plan's subscribers are dropped and passed
    * to `onTakenOver`. */
  def lock(planID: PlanID, onTakenOver: Set[Port] => Unit): EventStream[Either[String, Unit]] =
    if (isLocked(planID))
      EventStream.fromValue(Right(()), emitOnce = true)
    else
      locks
        .steal(lockName(planID), () => if (held.remove(planID).nonEmpty) onTakenOver(dropAll(planID)))
        .asObservable
        .recoverToTry
        .map {
          case Success(lock) => Right(held += planID -> lock)
          case Failure(error) => Left(describe(error))
        }

  /** Whether this coordinator holds the plan's lock. A lock can be lost to another coordinator
    * while waiting to [[register]], such as while reading the plan. */
  def isLocked(planID: PlanID): Boolean =
    held.contains(planID)

  def register(port: Port, planID: PlanID): LamportTimestamp =
    subscriptions.get(planID) match {
      case Some((lamport, ports)) =>
        subscriptions.update(planID, (lamport, ports + port))
        lamport

      case None =>
        val lamport = LamportTimestamp.initial
        subscriptions += planID -> (lamport, Set(port))
        lamport
    }

  /** Releases the plan's lock once it has no subscribers left */
  def deregister(port: Port, planID: PlanID): Unit = {
    subscriptions.get(planID).foreach { (lamport, ports) =>
      val updatedPorts = ports - port
      if (updatedPorts.isEmpty)
        subscriptions -= planID
      else
        subscriptions.update(planID, (lamport, updatedPorts))
    }
    releaseIfUnused(planID)
  }

  /** Releases the plan's lock if it has no subscribers, such as when a subscription fails after
    * the lock was taken */
  def releaseIfUnused(planID: PlanID): Unit =
    if (!subscriptions.contains(planID))
      held.remove(planID).foreach(_.release())

  /** Runs `f` if the plan isn't open in any version of the app, holding the plan's lock while
    * it does. Otherwise returns `ifInUse`, or `ifFailed` if the lock can't be requested. */
  def whileUnused[T](planID: PlanID)(ifInUse: => T, ifFailed: String => T)(
    f: => EventStream[T]
  ): EventStream[T] =
    if (subscriptions.contains(planID) || isLocked(planID))
      EventStream.fromValue(ifInUse, emitOnce = true)
    else
      locks.ifAvailable(lockName(planID)).asObservable.recoverToTry.flatMapSwitch {
        case Success(None) =>
          EventStream.fromValue(ifInUse, emitOnce = true)
        case Success(Some(lock)) =>
          f.map { result => lock.release(); result }
        case Failure(error) =>
          EventStream.fromValue(ifFailed(describe(error)), emitOnce = true)
      }

  def all: List[(PlanID, (LamportTimestamp, Set[Port]))] =
    subscriptions.toList

  def get(planID: PlanID): Option[(LamportTimestamp, Set[Port])] =
    subscriptions.get(planID)

  def incrementLamport(planID: PlanID): Unit =
    subscriptions.get(planID).foreach { (lamport, ports) =>
      subscriptions.update(planID, (lamport.increment, ports))
    }

  private def dropAll(planID: PlanID): Set[Port] =
    subscriptions.remove(planID).map((_, ports) => ports).getOrElse(Set.empty)

  private def lockName(planID: PlanID): String =
    s"leagueplans-plan-$planID"

  private def describe(error: Throwable): String =
    Option(error.getMessage).getOrElse(error.toString)
}
