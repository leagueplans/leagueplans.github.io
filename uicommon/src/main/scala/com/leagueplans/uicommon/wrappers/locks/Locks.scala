package com.leagueplans.uicommon.wrappers.locks

import org.scalajs.dom.{LockManager, LockOptions}

import scala.scalajs.js

/** Named locks, shared by every page and worker on the origin. The Web Locks API is available
  * in workers too, but `scala-js-dom` doesn't expose it on `WorkerNavigator`. */
trait Locks {
  /** Takes the lock, forcibly releasing it from any current holder, and holds it until it's
    * released. If the lock is later stolen from us in turn, `onLost` is called. */
  def steal(name: String, onLost: () => Unit): js.Promise[Locks.Held]

  /** Takes the lock only if nobody else holds it */
  def ifAvailable(name: String): js.Promise[Option[Locks.Held]]
}

object Locks {
  final class Held(onRelease: () => Unit) {
    def release(): Unit = onRelease()
  }

  val web: Locks = new Locks {
    private def manager: LockManager =
      js.Dynamic.global.navigator.locks.asInstanceOf[LockManager]

    def steal(name: String, onLost: () => Unit): js.Promise[Held] =
      hold(name, new LockOptions { this.steal = true }, onLost).`then`[Held](_.get)

    def ifAvailable(name: String): js.Promise[Option[Held]] =
      hold(name, new LockOptions { this.ifAvailable = true }, onLost = () => ())

    // The lock is held until the promise returned to the lock manager settles. The promise
    // returned by `request` rejects if the lock is stolen while we hold it.
    private def hold(name: String, options: LockOptions, onLost: () => Unit): js.Promise[Option[Held]] =
      new js.Promise[Option[Held]]((resolveAcquired, _) =>
        manager
          .request(name, options, lock =>
            if (lock == null) {
              resolveAcquired(None)
              js.Promise.resolve[Unit](())
            } else
              new js.Promise[Unit]((resolveHeld, _) =>
                resolveAcquired(Some(Held(() => resolveHeld(()): Unit)))
              )
          )
          .`then`[Unit](_ => (), (_: Any) => onLost())
      )
  }
}
