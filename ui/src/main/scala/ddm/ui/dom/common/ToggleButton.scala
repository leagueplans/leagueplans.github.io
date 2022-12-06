package ddm.ui.dom.common

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}

object ToggleButton {
  def apply[T](
    initial: T,
    alternative: T,
    initialContent: L.HtmlElement,
    alternativeContent: L.HtmlElement
  ): (L.Button, Signal[T]) = {
    val state = Var(initial)

    val button =
      L.button(
        L.`type`("button"),
        L.child <-- state.signal.map(t => if (t == initial) initialContent else alternativeContent),
        L.composeEvents(L.onClick)(_.filter(!_.defaultPrevented)) -->
          state.updater[Any]((t, _) => if (t == initial) alternative else initial)
      )

    (button, state.signal)
  }
}
