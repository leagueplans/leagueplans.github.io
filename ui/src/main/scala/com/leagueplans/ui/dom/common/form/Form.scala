package com.leagueplans.ui.dom.common.form

import com.leagueplans.ui.utils.laminar.LaminarOps.handled
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Form {
  def apply(): (L.FormElement, L.Input, EventStream[Unit]) = {
    val onSubmit = EventBus[Unit]()
    val submit = L.input(L.cls(Styles.submit), L.`type`("submit"))
    val form = L.form(L.onSubmit.handled --> onSubmit)
    (form, submit, onSubmit.events)
  }

  @js.native @JSImport("/styles/common/form/form.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val submit: String = js.native
  }
}
