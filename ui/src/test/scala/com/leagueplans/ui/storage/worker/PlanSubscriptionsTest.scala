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

final class PlanSubscriptionsTest extends AsyncFreeSpec with Matchers {
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

  private def subscriptions(locks: Locks = FakeLocks()): PlanSubscriptions[String] =
    PlanSubscriptions[String](locks)

  private def noTakeOver(ports: Set[String]): Unit = ()

  "PlanSubscriptions" - {
    "lock" - {
      "takes the plan's lock" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        first(subs.lock(planID, noTakeOver)).map { result =>
          result shouldEqual Right(())
          subs.isLocked(planID) shouldBe true
          locks.steals should have size 1
        }
      }

      "doesn't request a lock it already holds" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        for {
          _ <- first(subs.lock(planID, noTakeOver))
          _ <- first(subs.lock(planID, noTakeOver))
        } yield locks.steals should have size 1
      }

      "reports why the lock couldn't be taken" in {
        val subs = subscriptions(FakeLocks(failing = true))
        first(subs.lock(planID, noTakeOver)).map { result =>
          result shouldEqual Left("No locks here")
          subs.isLocked(planID) shouldBe false
        }
      }

      "drops the plan's subscribers if another coordinator takes the lock" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        var takenOver = Set.empty[String]
        first(subs.lock(planID, takenOver = _)).map { _ =>
          subs.register("a", planID)
          subs.register("b", planID)
          locks.stealFromUs(locks.steals.head)
          takenOver shouldEqual Set("a", "b")
          subs.get(planID) shouldBe None
          subs.isLocked(planID) shouldBe false
        }
      }
    }

    "deregister" - {
      "releases the lock when the last subscriber leaves, without reporting a takeover" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        var takenOver = false
        first(subs.lock(planID, _ => takenOver = true)).map { _ =>
          subs.register("a", planID)
          subs.register("b", planID)
          subs.deregister("a", planID)
          locks.releases shouldBe empty
          subs.deregister("b", planID)
          locks.releases should have size 1
          subs.isLocked(planID) shouldBe false
          locks.stealFromUs(locks.steals.head)
          takenOver shouldBe false
        }
      }
    }

    "releaseIfUnused" - {
      "releases a lock taken for a subscription that never happened" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        first(subs.lock(planID, noTakeOver)).map { _ =>
          subs.releaseIfUnused(planID)
          locks.releases should have size 1
          subs.isLocked(planID) shouldBe false
        }
      }

      "keeps the lock while the plan has subscribers" in {
        val locks = FakeLocks()
        val subs = subscriptions(locks)
        first(subs.lock(planID, noTakeOver)).map { _ =>
          subs.register("a", planID)
          subs.releaseIfUnused(planID)
          locks.releases shouldBe empty
          subs.isLocked(planID) shouldBe true
        }
      }
    }

    "whileUnused" - {
      var ran = false
      def run(subs: PlanSubscriptions[String]): Future[String] = {
        ran = false
        first(subs.whileUnused(planID)(
          ifInUse = "in use",
          ifFailed = reason => s"failed: $reason"
        ) {
          ran = true
          EventStream.fromValue("ran", emitOnce = true)
        })
      }

      "runs the action while holding the lock, then releases it" in {
        val locks = FakeLocks(available = true)
        run(subscriptions(locks)).map { result =>
          result shouldEqual "ran"
          locks.releases should have size 1
        }
      }

      "doesn't run the action if another coordinator holds the lock" in {
        run(subscriptions(FakeLocks(available = false))).map { result =>
          result shouldEqual "in use"
          ran shouldBe false
        }
      }

      "doesn't run the action if the plan has subscribers here" in {
        val subs = subscriptions()
        for {
          _ <- first(subs.lock(planID, noTakeOver))
          _ = subs.register("a", planID)
          result <- run(subs)
        } yield {
          result shouldEqual "in use"
          ran shouldBe false
        }
      }

      "doesn't run the action while a subscription is being set up here" in {
        val subs = subscriptions()
        for {
          _ <- first(subs.lock(planID, noTakeOver))
          result <- run(subs)
        } yield {
          result shouldEqual "in use"
          ran shouldBe false
        }
      }

      "doesn't run the action if the lock can't be requested" in {
        run(subscriptions(FakeLocks(failing = true))).map { result =>
          result shouldEqual "failed: No locks here"
          ran shouldBe false
        }
      }
    }
  }
}
