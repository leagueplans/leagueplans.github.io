package com.leagueplans.ui.dom.planning.player.league

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.planning.player.task.TaskPanel
import com.leagueplans.ui.model.plan.Effect.CompleteLeagueTask
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueTaskPanel {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteLeagueTask]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div = {
    val completeTasksSignal = playerSignal.map(_.leagueStatus.completedTasks)
    val leagueVar = Var(Option.empty[Mode.League])

    val toSummaryTab =
      (toggleObserver: Observer[Unit]) =>
        LeagueSummaryTab(Observer.combine(
          toggleObserver.contramap[Mode.League](_ => ()),
          leagueVar.someWriter
        ))

    TaskPanel(
      L.headerTag(
        L.img(L.cls(Styles.titleIcon), L.src(icon), L.alt("League points icon")),
        "Tasks"
      ),
      toSummaryTab,
      LeagueDetailsTab(
        completeTasksSignal,
        cache,
        effectObserverSignal,
        contextMenuController,
        leagueVar
      )
    )
  }

  @js.native @JSImport("/images/league-points-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/league/leagueTaskPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val titleIcon: String = js.native
  }
}
