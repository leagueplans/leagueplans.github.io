package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.uicommon.dom.*
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TotalLevelPane {
  def apply(stats: Signal[Stats], tooltip: Tooltip): L.Div =
    L.div(
      L.cls(Styles.total),
      L.text <-- stats.map(s => s"Total level: ${s.totalLevel}"),
      tooltip.register(toTooltipContents(stats), FloatingConfig.basicTooltip(Placement.right))
    )

  @js.native @JSImport("/styles/planning/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tooltip: String = js.native
    val total: String = js.native
  }

  private def toTooltipContents(stats: Signal[Stats]): L.Span =
    L.span(
      L.cls(Styles.tooltip),
      L.text <-- stats.map(s => s"Total XP: ${s.totalExp}")
    )
}
