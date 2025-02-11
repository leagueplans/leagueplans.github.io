package com.leagueplans.ui.dom.player.task

import com.raquo.laminar.api.{L, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskSummaryTab {
  def apply(categories: List[L.HtmlElement]): L.Div =
    L.div(
      L.cls(Styles.categories),
      categories.map(_.amend(L.cls(Styles.category)))
    )

  @js.native @JSImport("/styles/player/task/taskSummaryTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val categories: String = js.native
    val category: String = js.native
  }
}
