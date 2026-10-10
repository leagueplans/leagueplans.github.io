package com.leagueplans.uicommon.wrappers.locks

import org.scalajs.dom.{LockManager, LockOptions}

import scala.scalajs.js

/** Named locks, shared by every page and worker on the origin. The Web Locks API is available
  * in workers too, but `scala-js-dom` doesn't expose it on `WorkerNavigator`. */
trait Locks {
  /** Takes the lock, forcibly releasing it from any current holder, and holds it until it's
    * released. If the lock is later stolen from us in turn, `onLost` is called. The promise
    * rejects if the lock can't be taken. */
  def steal(name: String, onLost: () => Unit): js.Promise[Locks.Held]

  /** Takes the lock only if nobody else holds it. The promise rejects if the lock can't be
    * requested. */
  def ifAvailable(name: String): js.Promise[Option[Locks.Held]]
}

object Locks {
  final class Held(onRelease: () => Unit, settled: => js.Promise[Unit]) {
    /** Asks for the lock to be released. The lock manager releases it asynchronously, so another
      * request for the lock can still find it held until [[released]] settles. */
    def release(): Unit = onRelease()

    /** Settles once the lock manager has finished with the lock, whether because it was
      * released or because it was stolen */
    def released: js.Promise[Unit] = settled
  }

  val web: Locks =
    Locks(js.Dynamic.global.navigator.locks.asInstanceOf[LockManager])

  def apply(manager: => LockManager): Locks = new Locks {
    def steal(name: String, onLost: () => Unit): js.Promise[Held] =
      hold(name, new LockOptions { this.steal = true }, onLost).`then`[Held](_.get)

    def ifAvailable(name: String): js.Promise[Option[Held]] =
      hold(name, new LockOptions { this.ifAvailable = true }, onLost = () => ())

    // The lock is held until the promise returned to the lock manager settles. The promise
    // returned by `request` rejects either if the request fails, in which case the lock was
    // never granted, or if the lock is stolen while we hold it.
    private def hold(name: String, options: LockOptions, onLost: () => Unit): js.Promise[Option[Held]] =
      new js.Promise[Option[Held]]((resolveAcquired, rejectAcquired) => {
        var granted = false
        // The lock can be granted before `request` returns, so `Held` reads this lazily
        var settled: js.Promise[Unit] = js.Promise.resolve[Unit](())
        settled = manager
          .request(name, options, lock =>
            if (lock == null) {
              resolveAcquired(None)
              js.Promise.resolve[Unit](())
            } else {
              granted = true
              new js.Promise[Unit]((resolveHeld, _) =>
                resolveAcquired(Some(Held(() => resolveHeld(()): Unit, settled)))
              )
            }
          )
          .`then`[Unit](
            _ => (),
            (error: Any) => if (granted) onLost() else rejectAcquired(error): Unit
          )
      })
  }
}
