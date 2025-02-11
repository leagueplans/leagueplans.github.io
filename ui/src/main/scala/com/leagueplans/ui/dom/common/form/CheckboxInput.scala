package com.leagueplans.ui.dom.common.form

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}

object CheckboxInput {
  def apply(id: String, initial: Boolean): (L.Input, L.Label, Signal[Boolean]) = {
    val state = Var(initial)
    (input(id, initial, state), label(id), state.signal)
  }

  private def input(
    id: String,
    initial: Boolean,
    content: Var[Boolean]
  ): L.Input =
    L.input(
      L.`type`("checkbox"),
      L.idAttr(id),
      L.nameAttr(id),
      L.checked(initial),
      L.onClick.mapToChecked --> content
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
