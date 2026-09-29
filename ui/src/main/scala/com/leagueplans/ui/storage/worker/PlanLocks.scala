package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.utils.airstream.JsPromiseOps.asObservable
import com.leagueplans.uicommon.wrappers.locks.Locks
import com.raquo.airstream.core.EventStream
import org.scalajs.dom.console

import scala.collection.mutable
import scala.util.{Failure, Success}

/** Locks on the plans that this coordinator has subscribers for.
  *
  * Each deployed version of the app runs its own coordinator, because the coordinator's script
  * has a different name in each build. Without these locks, tabs on different versions could
  * edit the same plan without seeing each other's changes. When two versions want the same
  * plan, the one that asked most recently wins, and the other's subscribers are told the plan
  * was taken over. */
final class PlanLocks(locks: Locks) {
  private val held = mutable.Map.empty[PlanID, Locks.Held]

  def isHeld(planID: PlanID): Boolean =
    held.contains(planID)

  /** Takes the plan's lock if this coordinator doesn't already hold it. `onLost` is called if
    * another coordinator later takes it.
    *
    * If the lock can't be taken at all, the plan is left unlocked rather than unusable. */
  def steal(planID: PlanID, onLost: () => Unit): EventStream[Unit] =
    if (isHeld(planID))
      EventStream.fromValue((), emitOnce = true)
    else
      locks
        .steal(name(planID), () => if (held.remove(planID).nonEmpty) onLost())
        .asObservable
        .recoverToTry
        .map {
          case Success(lock) => held += planID -> lock
          case Failure(error) => console.warn(s"Unable to lock plan $planID", error)
        }

  def release(planID: PlanID): Unit =
    held.remove(planID).foreach(_.release())

  /** Runs `f` while holding the plan's lock, or returns `ifUnavailable` if any coordinator,
    * including this one, holds it.
    *
    * If the lock can't be requested at all, `f` runs without it. */
  def whileAvailable[T](planID: PlanID)(ifUnavailable: => T)(f: => EventStream[T]): EventStream[T] =
    if (isHeld(planID))
      EventStream.fromValue(ifUnavailable, emitOnce = true)
    else
      locks.ifAvailable(name(planID)).asObservable.recoverToTry.flatMapSwitch {
        case Success(None) =>
          EventStream.fromValue(ifUnavailable, emitOnce = true)
        case Success(Some(lock)) =>
          f.map { result => lock.release(); result }
        case Failure(error) =>
          console.warn(s"Unable to check the lock on plan $planID", error)
          f
      }

  private def name(planID: PlanID): String =
    s"leagueplans-plan-$planID"
}
