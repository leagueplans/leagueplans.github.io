package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.planning.player.league.LeagueTaskPanel
import com.leagueplans.ui.model.player.Player
import com.leagueplans.uicommon.dom.KeyValuePairs
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueSection {
  def apply(ctx: SectionContext): L.Div =
    L.div(
      L.cls(Styles.section),
      L.child <-- ctx.displayedPlayer.map(stats),
      LeagueTaskPanel(
        ctx.displayedPlayer,
        ctx.cache,
        ctx.effectObserver,
        ctx.tooltip,
        ctx.contextMenu
      ).amend(L.cls(Styles.tasksPanel))
    )

  @js.native @JSImport("/styles/planning/section/leagueSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val statsPanel: String = js.native
    val tasksPanel: String = js.native
  }

  private def stats(player: Player): ReactiveHtmlElement[DList] =
    KeyValuePairs(
      L.span("Tasks completed:") -> L.span(player.leagueStatus.completedTasks.size),
      L.span("League points:") -> L.span(player.leagueStatus.leaguePoints)
    ).amend(L.cls(Styles.statsPanel))
}
