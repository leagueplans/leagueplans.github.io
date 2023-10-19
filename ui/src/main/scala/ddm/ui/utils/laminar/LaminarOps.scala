package ddm.ui.utils.laminar

import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.eventPropToProcessor
import com.raquo.laminar.keys.{EventProp, LockedEventKey}
import ddm.ui.facades.fontawesome.commontypes.IconDefinition
import org.scalajs.dom.Event

import scala.util.chaining.scalaUtilChainingOps

object LaminarOps {
  implicit final class RichL(val self: L.type) extends AnyVal {
    def icon(definition: IconDefinition): L.SvgElement =
      FontAwesome.icon(definition)
  }

  implicit final class RichEventProp[E <: Event](val self: EventProp[E]) extends AnyVal {
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
}
