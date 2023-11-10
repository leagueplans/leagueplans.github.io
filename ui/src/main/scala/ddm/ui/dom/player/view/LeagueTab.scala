package ddm.ui.dom.player.view

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{ContextMenu, KeyValuePairs}
import ddm.ui.dom.player.league.LeagueTaskPanel
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.{Cache, Player}
import org.scalajs.dom.html.DList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LeagueTab {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.tabContent),
      L.child <-- playerSignal.map(stats),
      LeagueTaskPanel(playerSignal, cache, effectObserverSignal, contextMenuController).amend(L.cls(Styles.tasksPanel))
    )

  @js.native @JSImport("/styles/player/view/leagueTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val junkPanel: String = js.native
    val tasksPanel: String = js.native
  }

  private def stats(player: Player): ReactiveHtmlElement[DList] =
    KeyValuePairs(
      L.span("Multiplier:") -> L.span(player.leagueStatus.multiplierUsing(player.mode.expMultiplierStrategy)),
      L.span("Tasks completed:") -> L.span(player.leagueStatus.completedTasks.size),
      L.span("League points:") -> L.span(player.leagueStatus.leaguePoints)
    ).amend(L.cls(Styles.junkPanel))
}
