package ddm.ui.component.player

import ddm.ui.component.common.TextBasedTable
import ddm.ui.model.player.league.LeagueStatus
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object LeagueComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = LeagueStatus

  private def render(leagueStatus: LeagueStatus): VdomNode =
    TextBasedTable.build(List(
      "Multiplier:" -> leagueStatus.multiplier.toString,
      "League points:" -> leagueStatus.leaguePoints.toString,
      "Expected renown:" -> leagueStatus.expectedRenown.toString
    ))
}
