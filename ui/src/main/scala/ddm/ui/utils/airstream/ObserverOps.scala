package ddm.ui.utils.airstream

import com.raquo.airstream.core.Observer
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.ownership.Owner

object ObserverOps {
  implicit final class RichOptionObserver[T](val self: Option[Observer[T]]) extends AnyVal {
    def observer: Observer[T] =
      Observer[T](t => self.foreach(_.onNext(t)))
  }

  implicit final class RichSignalObserver[T](val self: SignalSource[Observer[T]]) extends AnyVal {
    def latest(owner: Owner): Observer[T] =
      Observer[T](t =>
        self.toObservable.observe(owner).tryNow().foreach(_.onNext(t))
      )
  }
}
