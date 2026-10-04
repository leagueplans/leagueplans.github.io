package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.quest.QuestList
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestsSection {
  def apply(ctx: SectionContext): L.Div =
    L.div(
      L.cls(Styles.section),
      QuestList(ctx.displayedPlayer, ctx.cache, ctx.effectObserver, ctx.contextMenu).amend(L.cls(Styles.panel))
    )

  @js.native @JSImport("/styles/planning/section/questsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val panel: String = js.native
  }
}
