package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.stats.StatsElement
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object SkillsSection {
  // The stats panel takes the multipliers as a plain value, so it's rebuilt when they change
  def apply(ctx: SectionContext): L.Div =
    L.div(
      L.cls(Styles.section),
      L.child <-- ctx.settings.map(_.expMultipliers).distinct.map(multipliers =>
        StatsElement(
          ctx.displayedPlayer,
          ctx.effectObserver,
          multipliers,
          ctx.cache,
          ctx.tooltip,
          ctx.modal
        )
      )
    )

  @js.native @JSImport("/styles/planning/section/skillsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
  }
}
