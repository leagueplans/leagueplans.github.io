package com.leagueplans.ui.utils.airstream

import com.raquo.airstream.core.{BaseObservable, EventStream, Observable}
import com.raquo.airstream.{BufferedStream, DelayedToggleStream, KillSwitchedStream}

object ObservableOps {
  extension [F[+_] <: Observable[?], S, T](self: BaseObservable[F, (S, T)]) {
    def unzip: (F[S], F[T]) = {
      val s: F[S] = self.map[S]((s, _) => s)
      val t: F[T] = self.map[T]((_, t) => t)
      (s, t)
    }
  }

  extension [F[X] <: Observable[X], T](self: F[T]) {
    def flatMapConcat[S](f: T => EventStream[S]): BufferedStream[T, S] =
      new BufferedStream[T, S](self, f)

    def withKillSwitch(resetOnStop: Boolean): KillSwitchedStream[T] =
      new KillSwitchedStream[T](self, resetOnStop)

    def withDelayedToggling[ID](getAction: T => DelayedToggleStream.Action[ID]): DelayedToggleStream[T, ID] =
      DelayedToggleStream(self, getAction)
  }
}
