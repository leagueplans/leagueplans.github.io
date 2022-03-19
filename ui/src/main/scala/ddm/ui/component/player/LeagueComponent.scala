package ddm.ui.component.player

import ddm.ui.component.common.DualColumnListComponent
import ddm.ui.model.player.league.LeagueStatus
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object LeagueComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  type Props = LeagueStatus

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props): VdomNode =
      dualColumnListComponent(List(
        ("Multiplier:", props.multiplier),
        ("League points:", props.leaguePoints),
        ("Expected renown:", props.expectedRenown)
      ))
  }
}
