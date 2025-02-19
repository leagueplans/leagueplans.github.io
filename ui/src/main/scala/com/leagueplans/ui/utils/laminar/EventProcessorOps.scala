package com.leagueplans.ui.utils.laminar

import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.keys.{EventProcessor, LockedEventKey}
import org.scalajs.dom.Event

object EventProcessorOps {
  extension [E <: Event, V <: Event](self: EventProcessor[E, V]) {
    def handled: EventProcessor[E, Unit] =
      handledAs(())

    def handledAs[T](t: => T): EventProcessor[E, T] =
      ifUnhandled.preventDefault.mapTo(t)

    def handledWith[T](f: EventStream[V] => Observable[T]): LockedEventKey[E, V, T] =
      ifUnhandled.preventDefault(f)

    def ifUnhandled: EventProcessor[E, V] =
      self.filter(!_.defaultPrevented)
  }
}
