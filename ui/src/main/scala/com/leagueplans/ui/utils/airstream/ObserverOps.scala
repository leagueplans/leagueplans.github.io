package com.leagueplans.ui.utils.airstream

import com.raquo.airstream.core.Observer
import com.raquo.airstream.core.Source.SignalSource
import com.raquo.airstream.ownership.Owner

object ObserverOps {
  extension [T](self: Option[Observer[T]]) {
    def observer: Observer[T] =
      Observer[T](t => self.foreach(_.onNext(t)))
  }

  extension [T](self: SignalSource[Observer[T]]) {
    def latest(owner: Owner): Observer[T] =
      Observer[T](t =>
        self.toObservable.observe(owner).tryNow().foreach(_.onNext(t))
      )
  }
}
