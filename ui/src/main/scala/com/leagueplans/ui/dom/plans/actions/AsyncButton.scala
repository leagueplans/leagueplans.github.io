package com.leagueplans.ui.dom.plans.actions

import com.leagueplans.uicommon.dom.{Button, LoadingIcon}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A button for work that takes a moment, such as reading a plan from storage. Clicks while it's
  * working are ignored. A spinner stands in for the label only once the work has taken long enough
  * to notice, and the button keeps its width, so that quick work doesn't make it flicker.
  */
object AsyncButton {
  private val spinnerDelayMillis = 300

  /** @param run does the work, emitting once when it's done */
  def apply(label: String, run: () => EventStream[Unit]): L.Button =
    withContent(label, run)

  /** @param content what the button shows when it isn't busy, such as an icon
    * @param run does the work, emitting once when it's done
    */
  def withContent(content: L.Modifier[L.Span], run: () => EventStream[Unit]): L.Button = {
    val isBusy = Var(false)
    val clicks = EventBus[Unit]()
    val showsBusy =
      isBusy.signal.changes.flatMapSwitch(busy =>
        if (busy) EventStream.delay(spinnerDelayMillis, true) else EventStream.fromValue(false)
      ).toSignal(false)

    Button(_.handled --> Observer[Unit] { _ =>
      if (!isBusy.now()) {
        isBusy.set(true)
        clicks.emit(())
      }
    }).amend(
      L.cls(Styles.button),
      L.aria.busy <-- isBusy.signal,
      L.disabled <-- showsBusy,
      L.span(L.cls(Styles.hidden) <-- showsBusy, content),
      L.child.maybe <-- showsBusy.map(Option.when(_)(LoadingIcon().amend(L.svg.cls(Styles.spinner)))),
      clicks.events.flatMapSwitch(_ => run()) --> Observer[Unit](_ => isBusy.set(false))
    )
  }

  @js.native @JSImport("/styles/plans/asyncButton.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val button: String = js.native
    val hidden: String = js.native
    val spinner: String = js.native
  }
}
