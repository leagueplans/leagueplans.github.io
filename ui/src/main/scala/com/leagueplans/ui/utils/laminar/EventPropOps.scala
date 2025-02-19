package com.leagueplans.ui.utils.laminar

import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.api.eventPropToProcessor
import com.raquo.laminar.keys.{EventProcessor, EventProp, LockedEventKey}
import org.scalajs.dom.Event

object EventPropOps {
  extension [E <: Event](self: EventProp[E]) {
    def handled: EventProcessor[E, Unit] =
      EventProcessorOps.handled(self)

    def handledAs[T](t: => T): EventProcessor[E, T] =
      EventProcessorOps.handledAs(self)(t)

    def handledWith[T](f: EventStream[E] => Observable[T]): LockedEventKey[E, E, T] =
      EventProcessorOps.handledWith(self)(f)

    def ifUnhandled: EventProcessor[E, E] =
      EventProcessorOps.ifUnhandled(self)
  }
}
