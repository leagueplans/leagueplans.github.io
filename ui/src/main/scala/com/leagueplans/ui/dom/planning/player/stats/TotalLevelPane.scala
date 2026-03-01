package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TotalLevelPane {
  def apply(stats: Signal[Stats], tooltip: Tooltip): L.Div =
    L.div(
      L.cls(Styles.pane),
      L.child <-- stats.splitOne(_.totalLevel)((level, _, _) =>
        L.span(L.cls(Styles.total), s"Total level:", L.br(), level)
      ),
      L.img(
        L.cls(Styles.background),
        L.src(background),
        L.alt("Total level")
      ),
      tooltip.register(toTooltipContents(stats), FloatingConfig.basicTooltip(Placement.right))
    )

  @js.native @JSImport("/images/stat-window/total-level-background.png", JSImport.Default)
  private val background: String = js.native

  @js.native @JSImport("/styles/planning/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val tooltip: String = js.native
    val background: String = js.native
    val total: String = js.native
  }

  private def toTooltipContents(stats: Signal[Stats]): L.Span =
    L.span(
      L.cls(Styles.tooltip),
      L.text <-- stats.map(s => s"Total XP: ${s.totalExp}")
    )
}
