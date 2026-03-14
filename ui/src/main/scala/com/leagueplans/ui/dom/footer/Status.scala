package com.leagueplans.ui.dom.footer

import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Observable
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Status {
  def apply(
    label: String,
    status: Observable[StatusTracker.Status],
    tooltip: Tooltip
  ): L.Div = {
    val indicator = L.div(
      L.cls <-- status.map {
        case StatusTracker.Status.Idle => Styles.idle
        case StatusTracker.Status.Busy => Styles.busy
        case _: StatusTracker.Status.Failed => Styles.failed
      }
    )

    L.div(
      L.cls(Styles.status),
      L.span(L.cls(Styles.label), label),
      indicator,
      tooltip.register(
        L.span(
          L.cls(Styles.tooltip),
          L.text <-- status.map {
            case StatusTracker.Status.Idle => "Idle"
            case StatusTracker.Status.Busy => "Working"
            case StatusTracker.Status.Failed(reason) => reason
          }
        ),
        FloatingConfig.basicAnchoredTooltip(
          anchor = indicator,
          Placement.top,
          includeArrow = true
        )
      )
    )
  }

  @js.native @JSImport("/styles/footer/status.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val status: String = js.native
    val label: String = js.native
    val tooltip: String = js.native

    val idle: String = js.native
    val busy: String = js.native
    val failed: String = js.native
  }
}
