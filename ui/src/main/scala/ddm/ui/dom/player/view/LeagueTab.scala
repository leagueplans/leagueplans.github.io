package ddm.ui.dom.player.view

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.{ContextMenu, KeyValuePairs}
import ddm.ui.dom.player.league.LeagueTaskPanel
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.{Cache, Player}

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
      L.child <-- playerSignal.map(p =>
        KeyValuePairs(
          L.span("Multiplier:") -> L.span(p.leagueStatus.multiplier),
          L.span("Tasks completed:") -> L.span(p.leagueStatus.completedTasks.size)
        ).amend(L.cls(Styles.junkPanel))
      ),
      LeagueTaskPanel(playerSignal, cache, effectObserverSignal, contextMenuController).amend(L.cls(Styles.tasksPanel))
    )

  @js.native @JSImport("/styles/player/view/leagueTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tabContent: String = js.native
    val junkPanel: String = js.native
    val tasksPanel: String = js.native
  }
}