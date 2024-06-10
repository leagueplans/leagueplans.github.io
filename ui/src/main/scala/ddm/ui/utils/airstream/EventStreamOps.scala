package ddm.ui.utils.airstream

import com.raquo.airstream.core.EventStream

object EventStreamOps {
  extension [E1, T](self: EventStream[Either[E1, T]]) {
    def andThen[E2 >: E1, S](
      f: T => EventStream[Either[E2, S]]
    ): EventStream[Either[E2, S]] =
      self.flatMap {
        case Left(error) => EventStream.fromValue(Left(error), emitOnce = true)
        case Right(value) => f(value)
      }
  }
}
