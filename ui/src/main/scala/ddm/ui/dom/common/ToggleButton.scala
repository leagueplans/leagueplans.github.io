package ddm.ui.dom.common

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.utils.laminar.LaminarOps.handled

object ToggleButton {
  def apply[T](
    initial: T,
    alternative: T,
    initialContent: L.Element,
    alternativeContent: L.Element
  ): (L.Button, Signal[T]) = {
    val isInitial = Var(true)
    val button =
      Button(isInitial.invertWriter)(_.handled).amend(
        L.child <-- isInitial.signal.splitBoolean(
          whenTrue = _ => initialContent,
          whenFalse = _ => alternativeContent
        )
      )

    val value = isInitial.signal.map {
      case true => initial
      case false => alternative
    }

    (button, value)
  }
}
