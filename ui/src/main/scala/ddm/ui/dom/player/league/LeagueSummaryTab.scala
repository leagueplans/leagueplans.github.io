package ddm.ui.dom.player.league

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L
import ddm.ui.dom.player.task.TaskSummaryTab
import ddm.ui.model.player.mode.Mode

object LeagueSummaryTab {
  def apply(leagueObserver: Observer[Mode.League]): L.Div =
    TaskSummaryTab(
      Mode.League.all.map(league =>
        LeagueOption(league, leagueObserver.contramap[Unit](_ => league))
      )
    )
}
