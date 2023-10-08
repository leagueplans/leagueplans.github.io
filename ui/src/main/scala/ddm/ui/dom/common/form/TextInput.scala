package ddm.ui.dom.common.form

import com.raquo.airstream.core.{Signal, Sink}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}

object TextInput {
  sealed trait Type

  object Type {
    case object Search extends Type
    case object Text extends Type
  }

  def apply(
    `type`: Type,
    id: String,
    initial: String
  ): (L.Input, L.Label, Signal[String]) = {
    val state = Var(initial)
    (input(`type`, id, initial, state), label(id), state.signal)
  }

  private def input(
    `type`: Type,
    id: String,
    initial: String,
    content: Sink[String]
  ): L.Input =
    L.input(
      L.`type`(`type` match {
        case Type.Search => "search"
        case Type.Text => "text"
      }),
      L.idAttr(id),
      L.name(id),
      L.defaultValue(initial),
      L.onInput.mapToValue.setAsValue --> content
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
