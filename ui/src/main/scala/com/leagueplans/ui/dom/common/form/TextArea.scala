package com.leagueplans.ui.dom.common.form

import com.raquo.airstream.core.Sink
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, eventPropToProcessor}

object TextArea {
  def apply(
    id: String,
    initial: String
  ): (L.TextArea, L.Label, StrictSignal[String]) = {
    val state = Var(initial)
    (input(id, initial, state), label(id), state.signal)
  }

  private def input(
    id: String,
    initial: String,
    content: Sink[String]
  ): L.TextArea =
    L.textArea(
      L.idAttr(id),
      L.nameAttr(id),
      L.defaultValue(initial),
      L.onInput.mapToValue.setAsValue --> content
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
