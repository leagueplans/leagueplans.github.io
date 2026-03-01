package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Observable
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object IconButtonModifiers {
  def apply(
    tooltipContents: String,
    screenReaderDescription: String,
    tooltip: Tooltip,
    tooltipPlacement: Placement,
    tooltipOffset: Double = 8
  ): L.Modifier[L.Button] =
    List(
      L.aria.label(screenReaderDescription),
      tooltip.register(
        L.span(L.cls(Styles.tooltip), tooltipContents),
        FloatingConfig.basicTooltip(tooltipPlacement, tooltipOffset)
      )
    )

  def using(
    tooltipContents: Observable[String],
    screenReaderDescription: Observable[String],
    tooltip: Tooltip,
    tooltipPlacement: Placement,
    tooltipOffset: Double = 8
  ): L.Modifier[L.Button] =
    List(
      L.aria.label <-- screenReaderDescription,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), L.span(L.text <-- tooltipContents)),
        FloatingConfig.basicTooltip(tooltipPlacement, tooltipOffset)
      )
    )

  @js.native @JSImport("/styles/common/iconButton.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tooltip: String = js.native
  }
}
