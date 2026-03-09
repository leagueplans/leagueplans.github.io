package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.ui.dom.common.LabelledBox
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object SectionV2 {
  def apply(title: String)(contents: L.Modifier[L.HtmlElement]*): L.Div =
    L.div(
      L.cls(Styles.container),
      LabelledBox(
        L.span(L.cls(Styles.title), title)
      ).amend(L.cls(Styles.contents), contents)
    )

  @js.native @JSImport("/styles/planning/editor/sectionV2.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val title: String = js.native
    val contents: String = js.native
  }
}
