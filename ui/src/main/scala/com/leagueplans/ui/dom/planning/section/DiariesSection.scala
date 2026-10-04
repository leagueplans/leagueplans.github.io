package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.diary.DiaryPanel
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiariesSection {
  def apply(ctx: SectionContext): L.Div =
    L.div(
      L.cls(Styles.section),
      DiaryPanel(ctx.displayedPlayer, ctx.cache, ctx.effectObserver, ctx.tooltip, ctx.contextMenu).amend(L.cls(Styles.panel))
    )

  @js.native @JSImport("/styles/planning/section/diariesSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val panel: String = js.native
  }
}
