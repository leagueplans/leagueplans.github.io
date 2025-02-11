package com.leagueplans.ui.utils.airstream

import com.raquo.airstream.core.EventStream

object EventStreamOps {
  extension [E1, T](self: EventStream[Either[E1, T]]) {
    def andThen[E2 >: E1, S](
      f: T => EventStream[Either[E2, S]]
    ): EventStream[Either[E2, S]] =
      self.flatMapSwitch {
        case Left(error) => EventStream.fromValue(Left(error), emitOnce = true)
        case Right(value) => f(value)
      }
  }

  extension (self: EventStream.type) {
    /* When fed an empty collection, the library's default behaviour is to not emit
     * any elements. */
    def safeSequence[T](streams: Seq[EventStream[T]]): EventStream[Seq[T]] =
      if (streams.isEmpty)
        EventStream.fromValue(List.empty)
      else
        EventStream.sequence(streams)
  }
}
