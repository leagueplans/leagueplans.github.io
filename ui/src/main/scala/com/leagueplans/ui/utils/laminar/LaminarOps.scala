package com.leagueplans.ui.utils.laminar

import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.api.{L, Laminar, eventPropToProcessor}
import com.raquo.laminar.keys.{EventProcessor, LockedEventKey}
import org.scalajs.dom.{Event, KeyboardEvent}

import scala.util.chaining.scalaUtilChainingOps

object LaminarOps {
  extension [E <: Event](self: EventProcessor[E, E]) {
    def handled: LockedEventKey[E, E, Unit] =
      handledAs(())

    def handledAs[T](t: => T): LockedEventKey[E, E, T] =
      ifUnhandledF(_.map { event =>
        event.preventDefault()
        t
      })
      
    def handledWith[T](f: EventStream[E] => Observable[T]): LockedEventKey[E, E, T] =
      ifUnhandledF(stream =>
        f(stream.map { event =>
          event.preventDefault()
          event
        })
      )

    def ifUnhandled: LockedEventKey[E, E, E] =
      ifUnhandledF(identity)

    def ifUnhandledF[T](
      f: EventStream[E] => Observable[T]
    ): LockedEventKey[E, E, T] =
      self.compose(_.filter(!_.defaultPrevented).pipe(f))
  }

  extension (self: Laminar) {
    def onKey(keyCode: Int): EventProcessor[KeyboardEvent, KeyboardEvent] =
      self.onKeyDown.filter(_.keyCode == keyCode)

    def onMountAnimate[E <: L.Element](f: E => Animation.Instance): self.Modifier[E] =
      self.onMountUnmountCallbackWithState[E, Animation.Instance](
        mountContext => f(mountContext.thisNode),
        (_, maybeAnimation) => maybeAnimation.foreach(_.cancel())
      )
  }
}
