package ddm.ui.dom.common.form

import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}

object NumberInput {
  def apply[N : Numeric](id: String, initial: N): (L.Input, L.Label, Signal[N]) = {
    val state = Var(initial)
    (input(id, state), label(id), state.signal)
  }

  private def input[N : Numeric](
    id: String,
    content: Var[N]
  ): L.Input =
    L.input(
      L.`type`("number"),
      L.idAttr(id),
      L.name(id),
      L.controlled(
        L.value <-- content.signal.map(_.toString),
        L.onInput.mapToValue.map(
          Numeric[N].parseString(_).getOrElse(Numeric[N].zero)
        ) --> content
      )
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
