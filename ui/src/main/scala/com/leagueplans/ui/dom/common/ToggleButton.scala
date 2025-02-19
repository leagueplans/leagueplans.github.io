package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

object ToggleButton {
  def apply[T](
    initial: T,
    alternative: T,
    initialContent: L.Element,
    alternativeContent: L.Element
  ): (L.Button, Signal[T]) = {
    val isInitial = Var(true)
    val button =
      Button(_.handled --> isInitial.invertWriter).amend(
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
