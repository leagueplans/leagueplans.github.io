package ddm.ui.dom.player.league

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.LeagueTask
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.CompleteLeagueTask
import ddm.ui.utils.laminar.LaminarOps.RichEventProp
import org.scalajs.dom.html.Button

object LeagueTaskContextMenu {
  def apply(
    leagueTask: LeagueTask,
    effectObserver: Observer[CompleteLeagueTask],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): ReactiveHtmlElement[Button] =
    L.button(
      L.`type`("button"),
      "Complete",
      L.onClick.handledAs(CompleteLeagueTask(leagueTask.id)) -->
        Observer.combine(effectObserver, menuCloser)
    )
}
