package com.leagueplans.ui.dom.common

import com.raquo.airstream.core.Observable
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

object IconButtonModifiers {
  def apply(
    tooltip: String,
    screenReaderDescription: String
  ): L.Modifier[L.Button] =
    List(
      L.aria.label(screenReaderDescription),
      Tooltip(L.span(tooltip))
    )

  def apply(
    tooltip: Observable[String],
    screenReaderDescription: Observable[String]
  ): L.Modifier[L.Button] =
    List(
      L.aria.label <-- screenReaderDescription,
      Tooltip(L.span(L.text <-- tooltip))
    )
}
