package ddm.ui.dom.common.form

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.Event

object Form {
  def apply(): (L.FormElement, L.Input, EventStream[Unit]) = {
    val onSubmit = new EventBus[Unit]
    val submit = input()
    val form = L.form(
      L.onSubmit.preventDefault --> onSubmit.writer.contramap[Event](_ => ())
    )
    (form, submit, onSubmit.events)
  }

  private def input(): L.Input =
    L.input(L.`type`("submit"))
}
