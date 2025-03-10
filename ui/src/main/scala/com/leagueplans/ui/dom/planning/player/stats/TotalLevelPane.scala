package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.player.skill.Stats
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TotalLevelPane {
  def apply(stats: Signal[Stats]): L.Div =
    toPane(stats).amend(toTooltip(stats))

  @js.native @JSImport("/images/stat-window/total-level-background.png", JSImport.Default)
  private val background: String = js.native

  @js.native @JSImport("/styles/planning/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val background: String = js.native
    val total: String = js.native
    val xp: String = js.native
  }

  private def toPane(stats: Signal[Stats]): L.Div =
    L.div(
      L.cls(Styles.pane),
      L.child <-- stats.splitOne(_.totalLevel)((level, _, _) =>
        L.span(L.cls(Styles.total), s"Total level:", L.br(), level)
      ),
      L.img(
        L.cls(Styles.background),
        L.src(background),
        L.alt("Total level")
      )
    )

  private def toTooltip(stats: Signal[Stats]): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("Total XP:") -> L.span(L.cls(Styles.xp), L.child.text <-- stats.map(_.totalExp.toString))
    ))
}
