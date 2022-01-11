package ddm.ui.component.player

import ddm.ui.model.player.league.LeagueStatus
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object LeagueComponent {
  def apply(leagueStatus: LeagueStatus): Unmounted[LeagueStatus, Unit, Unit] =
    ScalaComponent
      .builder[LeagueStatus]
      .render_P(ls =>
        <.div(
          <.p(s"Multiplier: ${ls.multiplier}"),
          <.p(s"League points: ${ls.leaguePoints}"),
          <.p(s"Expected renown: ${ls.expectedRenown}")
        )
      )
      .build
      .apply(leagueStatus)
}
