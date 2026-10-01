package com.leagueplans.ui.storage.worker

import com.leagueplans.uicommon.wrappers.locks.Locks
import org.scalajs.dom.{Lock, LockManager, LockOptions}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.util.Failure

/** Tests [[Locks]] against a fake lock manager, since the Web Locks API isn't available here. It
  * lives with the storage tests because uicommon has no test setup. */
final class LocksTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private type Callback = js.Function1[Lock, js.Promise[Unit]]

  private def manager(request: (String, LockOptions, Callback) => js.Promise[Unit]): LockManager =
    js.Dynamic.literal(
      request = (request(_, _, _)): js.Function3[String, LockOptions, Callback, js.Promise[Unit]]
    ).asInstanceOf[LockManager]

  private val aLock: Lock =
    js.Dynamic.literal(name = "lock", mode = "exclusive").asInstanceOf[Lock]

  /** Grants the lock. The request settles when the holder releases it, or when `steal` is called. */
  private final class GrantingManager {
    private var stealIt: () => Unit = () => ()

    val underlying: LockManager = manager((_, _, callback) =>
      new js.Promise[Unit]((resolve, reject) => {
        stealIt = () => reject(js.Error("AbortError")): Unit
        callback(aLock).`then`[Unit](_ => resolve(()): Unit): Unit
      })
    )

    def steal(): Unit = stealIt()
  }

  "Locks" - {
    "steal" - {
      "holds the lock until it's released, without reporting it as lost" in {
        val granting = GrantingManager()
        var lost = false
        Locks(granting.underlying).steal("lock", () => lost = true).toFuture.flatMap { held =>
          held.release()
          // Let the request settle before checking
          Future.unit.map(_ => lost shouldBe false)
        }
      }

      "reports the lock being stolen while it's held" in {
        val granting = GrantingManager()
        var lost = false
        Locks(granting.underlying).steal("lock", () => lost = true).toFuture.flatMap { _ =>
          granting.steal()
          Future.unit.map(_ => lost shouldBe true)
        }
      }

      "fails, rather than never finishing, if the request fails before the lock is granted" in {
        var lost = false
        val refusing = manager((_, _, _) => js.Promise.reject(js.Error("Refused")))
        Locks(refusing).steal("lock", () => lost = true).toFuture.transform {
          case Failure(_) => scala.util.Success(lost shouldBe false)
          case other => scala.util.Success(fail(s"Expected a failure, got $other"))
        }
      }

      "fails if there's no lock manager" in {
        Locks(throw js.JavaScriptException(js.Error("No navigator.locks"))).steal("lock", () => ())
          .toFuture.transform {
            case Failure(_) => scala.util.Success(succeed)
            case other => scala.util.Success(fail(s"Expected a failure, got $other"))
          }
      }
    }

    "ifAvailable" - {
      "returns nothing if another holder has the lock" in {
        val unavailable = manager { (_, _, callback) => callback(null); js.Promise.resolve[Unit](()) }
        Locks(unavailable).ifAvailable("lock").toFuture.map(_ shouldBe None)
      }

      "fails if the request fails" in {
        val refusing = manager((_, _, _) => js.Promise.reject(js.Error("Refused")))
        Locks(refusing).ifAvailable("lock").toFuture.transform {
          case Failure(_) => scala.util.Success(succeed)
          case other => scala.util.Success(fail(s"Expected a failure, got $other"))
        }
      }
    }
  }
}
