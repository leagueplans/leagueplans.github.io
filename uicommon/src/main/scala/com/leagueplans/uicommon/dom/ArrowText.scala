package com.leagueplans.uicommon.dom

import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Text with each → lifted to the middle of the digits and capitals around it, since the font
  * draws it nearer the baseline. An arrow stays on the line of what follows it, so a wrapped line
  * doesn't end in one. */
object ArrowText {
  def apply(text: String): List[L.Node] =
    text.split("→", -1).toList match {
      case first :: rest =>
        textToTextNode(first) :: rest.flatMap(after =>
          List(L.span(L.cls(Styles.arrow), "→"), textToTextNode(after.replaceFirst("^ ", "\u00a0")))
        )
      case Nil => List.empty
    }

  @js.native @JSImport("/styles/common/arrowText.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val arrow: String = js.native
  }
}
