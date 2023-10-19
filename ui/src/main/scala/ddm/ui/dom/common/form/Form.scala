package ddm.ui.dom.common.form

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L
import ddm.ui.utils.laminar.LaminarOps.RichEventProp

object Form {
  def apply(): (L.FormElement, L.Input, EventStream[Unit]) = {
    val onSubmit = new EventBus[Unit]
    val submit = L.input(L.`type`("submit"))
    val form = L.form(L.onSubmit.handled --> onSubmit.writer)
    (form, submit, onSubmit.events)
  }
}
