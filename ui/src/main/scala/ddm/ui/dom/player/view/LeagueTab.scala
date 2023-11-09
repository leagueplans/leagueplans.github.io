package ddm.ui.dom.player.view

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.LeagueTaskTier
import ddm.ui.dom.common.{ContextMenu, KeyValuePairs}
import ddm.ui.dom.player.league.LeagueTaskPanel
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.mode._
import ddm.ui.model.player.{Cache, Player}
import org.scalajs.dom.console
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueTab {
  def apply(
    playerSignal: Signal[Player],
    league: Mode.League,
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      L.child <-- playerSignal.map(stats(_, league, cache)),
      LeagueTaskPanel(playerSignal, cache, effectObserverSignal, contextMenuController).amend(L.cls(Styles.tasksPanel))
    )

  @js.native @JSImport("/styles/player/view/leagueTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val junkPanel: String = js.native
    val tasksPanel: String = js.native
  }

  private def stats(
    player: Player,
    league: Mode.League,
    cache: Cache,
  ): ReactiveHtmlElement[DList] =
    KeyValuePairs(
      L.span("Multiplier:") -> L.span(player.leagueStatus.multiplier),
      L.span("Tasks completed:") -> L.span(player.leagueStatus.completedTasks.size),
      L.span("League points:") -> L.span(calculatePoints(league, cache, player.leagueStatus.completedTasks))
    ).amend(L.cls(Styles.junkPanel))

  private def calculatePoints(
    league: Mode.League,
    cache: Cache,
    completedTasks: Set[Int]
  ): Int =
    completedTasks
      .toList
      .map { taskID =>
        val task = cache.leagueTasks(taskID)

        league match {
          case LeaguesI => task.leagues1Props.map(toLeagues1Points)
          case LeaguesII => task.leagues2Props.map(props => toLeagues2Points(props.tier))
          case LeaguesIII => task.leagues3Props.map(props => toLeagues3Points(props.tier))
          case LeaguesIV => task.leagues4Props.map(props => toLeagues4Points(props.tier))
          case other =>
            console.error(message = s"Unexpected game mode for league point calculation: [$other]")
            None
        }
      }
      .map(_.getOrElse(0))
      .sum

  private def toLeagues1Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 50
      case LeagueTaskTier.Hard => 100
      case LeagueTaskTier.Elite => 250
      case LeagueTaskTier.Master => 500
    }

  private def toLeagues2Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 50
      case LeagueTaskTier.Hard => 100
      case LeagueTaskTier.Elite => 250
      case LeagueTaskTier.Master => 500
    }

  private def toLeagues3Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 5
      case LeagueTaskTier.Easy => 5
      case LeagueTaskTier.Medium => 25
      case LeagueTaskTier.Hard => 50
      case LeagueTaskTier.Elite => 125
      case LeagueTaskTier.Master => 250
    }

  private def toLeagues4Points(tier: LeagueTaskTier): Int =
    tier match {
      case LeagueTaskTier.Beginner => 0
      case LeagueTaskTier.Easy => 10
      case LeagueTaskTier.Medium => 40
      case LeagueTaskTier.Hard => 80
      case LeagueTaskTier.Elite => 200
      case LeagueTaskTier.Master => 400
    }
}
