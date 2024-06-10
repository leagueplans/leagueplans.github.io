package ddm.ui.utils.airstream

import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.ownership.ManualOwner

import scala.annotation.nowarn

object PromiseLikeOps {
  extension [T](self: Signal[Option[T]]) {
    def onComplete(f: T => ?): Unit =
      self.changes.collectSome.onComplete(f)
  }

  extension [T](self: EventStream[T]) {
    def onComplete(f: T => ?): Unit = {
      val owner = new ManualOwner
      self.foreach { value =>
        f(value)
        owner.killSubscriptions()
      }(using owner): @nowarn("msg=discarded non-Unit value")
    }
  }
  
  extension [Error, T](self: Signal[Option[Either[Error, T]]]) {
    def onComplete(onError: Error => ?, onSuccess: T => ?): Unit =
      self.changes.collectSome.onComplete(onError, onSuccess)
  }

  extension [Error, T](self: EventStream[Either[Error, T]]) {
    def onComplete(onError: Error => ?, onSuccess: T => ?): Unit = {
      val owner = new ManualOwner
      self.foreach {
        case Left(error) =>
          onError(error)
          owner.killSubscriptions()
        case Right(value) =>
          onSuccess(value)
          owner.killSubscriptions()
      }(using owner): @nowarn("msg=discarded non-Unit value")
    }
  }
}
