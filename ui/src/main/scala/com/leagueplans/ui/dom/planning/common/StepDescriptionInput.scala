package com.leagueplans.ui.dom.planning.common

import com.leagueplans.ui.dom.common.form.TextArea
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.utils.laminar.LaminarOps.{onKey, selectOnFocus}
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.L
import org.scalajs.dom.KeyValue

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescriptionInput {
  def apply(id: String, initial: String)(
    onSubmit: Observer[Unit]
  ): (L.TextArea, L.Label, StrictSignal[String]) = {
    val (area, label, state) = TextArea(s"$id-step-description-input", initial)
    area.amend(
      L.cls(Styles.contents),
      L.required(true),
      L.placeholder("Chop some logs"),
      L.selectOnFocus,
      L.rows(4),
      L.onKey(KeyValue.Enter).filterNot(_.shiftKey).handled --> onSubmit
    )

    (area, label, state)
  }

  @js.native @JSImport("/styles/planning/common/stepDescriptionInput.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
  }
}
