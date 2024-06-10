package ddm.ui.utils.laminar

import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.api.L.eventPropToProcessor
import com.raquo.laminar.api.Laminar
import com.raquo.laminar.keys.{EventProp, LockedEventKey}
import com.raquo.laminar.nodes.ReactiveElement
import ddm.ui.facades.animation.Animation
import org.scalajs.dom.{Element, Event}

import scala.util.chaining.scalaUtilChainingOps

object LaminarOps {
  extension [E <: Event](self: EventProp[E]) {
    def handled: LockedEventKey[E, E, Unit] =
      handledAs(())

    def handledAs[T](t: => T): LockedEventKey[E, E, T] =
      ifUnhandledF(_.map { event =>
        event.preventDefault()
        t
      })

    def ifUnhandled: LockedEventKey[E, E, E] =
      ifUnhandledF(identity)

    def ifUnhandledF[Out](
      f: EventStream[E] => Observable[Out]
    ): LockedEventKey[E, E, Out] =
      self.compose(_.filter(!_.defaultPrevented).pipe(f))
  }
  
  extension (self: Laminar) {
    def onMountAnimate[El <: Element](f: El => Animation): self.Modifier[ReactiveElement[El]] =
      self.onMountUnmountCallbackWithState[ReactiveElement[El], Animation](
        mountContext => f(mountContext.thisNode.ref),
        (_, maybeAnimation) => maybeAnimation.foreach(_.cancel())
      )
  }
}
