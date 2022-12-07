package ddm.ui.utils.laminar

import com.raquo.airstream.core.{EventStream, Observable}
import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.EventProcessor
import com.raquo.laminar.keys.LockedEventKey
import ddm.ui.facades.fontawesome.commontypes.IconDefinition
import org.scalajs.dom.Event

import scala.util.chaining.scalaUtilChainingOps

object LaminarOps {
  implicit final class RichL(val self: L.type) extends AnyVal {
    def ifUnhandled[E1 <: Event, E2 <: Event](event: EventProcessor[E1, E2]): LockedEventKey[E1, E2, E2] =
      ifUnhandledF(event)(identity)

    def ifUnhandledF[E1 <: Event, E2 <: Event, Out](event: EventProcessor[E1, E2])(
      f: EventStream[E2] => Observable[Out]
    ): LockedEventKey[E1, E2, Out] =
      self.composeEvents(event)(_.filter(!_.defaultPrevented).pipe(f))

    def icon(definition: IconDefinition): L.SvgElement =
      FontAwesome.icon(definition)
  }
}
