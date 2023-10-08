package ddm.ui.dom.common

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.Event

object ToggleButton {
  def apply[T](
    initial: T,
    alternative: T,
    initialContent: L.Element,
    alternativeContent: L.Element
  ): (L.Button, Signal[T]) = {
    val state = Var(initial)

    val button =
      L.button(
        L.`type`("button"),
        L.child <-- state.signal.map(t => if (t == initial) initialContent else alternativeContent),
        L.ifUnhandled(L.onClick) --> state.updater[Event] { case (t, event) =>
          event.preventDefault()
          if (t == initial) alternative else initial
        }
      )

    (button, state.signal)
  }
}
