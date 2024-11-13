package ddm.ui.dom.player.league

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.LeagueTask
import ddm.ui.dom.common.{Button, ContextMenu}
import ddm.ui.model.plan.Effect.CompleteLeagueTask
import ddm.ui.utils.laminar.LaminarOps.handledAs

object LeagueTaskContextMenu {
  def apply(
    leagueTask: LeagueTask,
    effectObserver: Observer[CompleteLeagueTask],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      Observer.combine(effectObserver, menuCloser)
    )(_.handledAs(CompleteLeagueTask(leagueTask.id))).amend("Complete")
}
