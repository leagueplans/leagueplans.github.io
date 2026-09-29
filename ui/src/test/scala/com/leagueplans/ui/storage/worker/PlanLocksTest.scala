package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.uicommon.wrappers.locks.Locks
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

final class PlanLocksTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private val planID = PlanID.fromString("plan")

  /** Records requests, and lets tests decide availability and take locks away */
  private final class FakeLocks(available: Boolean = true, failing: Boolean = false) extends Locks {
    val steals = mutable.ListBuffer.empty[String]
    val releases = mutable.ListBuffer.empty[String]
    private val lostCallbacks = mutable.Map.empty[String, () => Unit]

    def steal(name: String, onLost: () => Unit): js.Promise[Locks.Held] =
      if (failing) js.Promise.reject("No locks here")
      else {
        steals += name
        lostCallbacks += name -> onLost
        js.Promise.resolve[Locks.Held](Locks.Held(() => releases += name))
      }

    def ifAvailable(name: String): js.Promise[Option[Locks.Held]] =
      if (failing) js.Promise.reject("No locks here")
      else js.Promise.resolve[Option[Locks.Held]](
        Option.when(available)(Locks.Held(() => releases += name))
      )

    def stealFromUs(name: String): Unit =
      lostCallbacks.remove(name).foreach(_())
  }

  private def first[T](stream: EventStream[T]): Future[T] = {
    val promise = Promise[T]()
    val owner = new ManualOwner
    stream.foreach { value =>
      promise.trySuccess(value)
      owner.killSubscriptions()
    }(using owner)
    promise.future
  }

  "PlanLocks" - {
    "steal" - {
      "holds the plan's lock" in {
        val locks = FakeLocks()
        val planLocks = PlanLocks(locks)
        first(planLocks.steal(planID, () => ())).map { _ =>
          planLocks.isHeld(planID) shouldBe true
          locks.steals should have size 1
        }
      }

      "doesn't request a lock this coordinator already holds" in {
        val locks = FakeLocks()
        val planLocks = PlanLocks(locks)
        for {
          _ <- first(planLocks.steal(planID, () => ()))
          _ <- first(planLocks.steal(planID, () => ()))
        } yield locks.steals should have size 1
      }

      "reports the lock being taken by another coordinator" in {
        val locks = FakeLocks()
        val planLocks = PlanLocks(locks)
        var lost = false
        first(planLocks.steal(planID, () => lost = true)).map { _ =>
          locks.stealFromUs(locks.steals.head)
          lost shouldBe true
          planLocks.isHeld(planID) shouldBe false
        }
      }

      "leaves the plan unlocked, rather than unusable, if locks don't work" in {
        val planLocks = PlanLocks(FakeLocks(failing = true))
        first(planLocks.steal(planID, () => ())).map(_ =>
          planLocks.isHeld(planID) shouldBe false
        )
      }
    }

    "release" - {
      "releases the lock without reporting it as lost" in {
        val locks = FakeLocks()
        val planLocks = PlanLocks(locks)
        var lost = false
        first(planLocks.steal(planID, () => lost = true)).map { _ =>
          planLocks.release(planID)
          locks.stealFromUs(locks.steals.head)
          locks.releases should have size 1
          planLocks.isHeld(planID) shouldBe false
          lost shouldBe false
        }
      }
    }

    "whileAvailable" - {
      "runs the action while holding the lock, then releases it" in {
        val locks = FakeLocks(available = true)
        first(PlanLocks(locks).whileAvailable(planID)("unavailable")(
          EventStream.fromValue("ran", emitOnce = true)
        )).map { result =>
          result shouldEqual "ran"
          locks.releases should have size 1
        }
      }

      "doesn't run the action if another coordinator holds the lock" in {
        var ran = false
        first(PlanLocks(FakeLocks(available = false)).whileAvailable(planID)("unavailable") {
          ran = true
          EventStream.fromValue("ran", emitOnce = true)
        }).map { result =>
          result shouldEqual "unavailable"
          ran shouldBe false
        }
      }

      "doesn't run the action if this coordinator holds the lock" in {
        val planLocks = PlanLocks(FakeLocks())
        for {
          _ <- first(planLocks.steal(planID, () => ()))
          result <- first(planLocks.whileAvailable(planID)("unavailable")(
            EventStream.fromValue("ran", emitOnce = true)
          ))
        } yield result shouldEqual "unavailable"
      }

      "runs the action anyway if locks don't work" in {
        first(PlanLocks(FakeLocks(failing = true)).whileAvailable(planID)("unavailable")(
          EventStream.fromValue("ran", emitOnce = true)
        )).map(_ shouldEqual "ran")
      }
    }
  }
}
