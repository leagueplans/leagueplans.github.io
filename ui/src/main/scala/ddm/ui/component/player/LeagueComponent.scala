package ddm.ui.component.player

import ddm.ui.model.player.league.LeagueStatus
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object LeagueComponent {
  def apply(leagueStatus: LeagueStatus): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(leagueStatus))

  final case class Props(leagueStatus: LeagueStatus)

  private def render(props: Props): VdomNode =
    <.div(
      <.p(s"Multiplier: ${props.leagueStatus.multiplier}"),
      <.p(s"League points: ${props.leagueStatus.leaguePoints}"),
      <.p(s"Expected renown: ${props.leagueStatus.expectedRenown}")
    )
}
