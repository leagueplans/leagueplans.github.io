package ddm.ui.dom.common.form

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.Event

object Form {
  def apply(): (L.FormElement, L.Input, EventStream[Unit]) = {
    val onSubmit = new EventBus[Unit]
    val submit = L.input(L.`type`("submit"))
    val form = L.form(
      L.ifUnhandled(L.onSubmit) --> onSubmit.writer.contramap[Event](_.preventDefault())
    )
    (form, submit, onSubmit.events)
  }
}
