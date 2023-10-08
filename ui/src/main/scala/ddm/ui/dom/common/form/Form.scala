package ddm.ui.dom.common.form

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, eventPropToProcessor}
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.{Event, MouseEvent}

object Form {
  def apply(): (L.FormElement, L.Input, EventStream[Unit]) = {
    val onSubmit = new EventBus[Unit]
    val submit = input(onSubmit.writer)
    val form = L.form(
      L.ifUnhandled(L.onSubmit) --> onSubmit.writer.contramap[Event](_.preventDefault())
    )
    (form, submit, onSubmit.events)
  }

  private def input(onSubmit: Observer[Unit]): L.Input =
    L.input(
      L.`type`("submit"),
      L.ifUnhandled(L.onClick) --> onSubmit.contramap[MouseEvent](_.preventDefault())
    )
}
