package com.leagueplans.ui.dom.planning.player.view

import com.leagueplans.ui.dom.common.{ContextMenu, KeyValuePairs, Tooltip}
import com.leagueplans.ui.dom.planning.player.league.LeagueTaskPanel
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueTab {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    tooltip: Tooltip,
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      L.child <-- playerSignal.map(stats),
      LeagueTaskPanel(
        playerSignal,
        cache,
        effectObserverSignal,
        tooltip,
        contextMenuController
      ).amend(L.cls(Styles.tasksPanel))
    )

  @js.native @JSImport("/styles/planning/player/view/leagueTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val junkPanel: String = js.native
    val tasksPanel: String = js.native
  }

  private def stats(player: Player): ReactiveHtmlElement[DList] =
    KeyValuePairs(
      L.span("Tasks completed:") -> L.span(player.leagueStatus.completedTasks.size),
      L.span("League points:") -> L.span(player.leagueStatus.leaguePoints)
    ).amend(L.cls(Styles.junkPanel))
}
