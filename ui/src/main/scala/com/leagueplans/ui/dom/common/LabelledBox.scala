package com.leagueplans.ui.dom.common

import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A content box with a title centered on the top border.
  *
  * It's helpful to define a margin-top on the container to avoid issues with
  * clipping of the label.
  */
object LabelledBox {
  def apply(label: L.HtmlElement): L.HtmlElement =
    L.sectionTag(
      L.cls(Styles.container),
      label.amend(L.cls(Styles.label))
    )

  @js.native @JSImport("/styles/common/labelledBox.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val container: String = js.native
    val label: String = js.native
  }
}
