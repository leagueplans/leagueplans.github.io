package ddm.ui.component.player

import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatusComponent {
  def apply(player: Player): Unmounted[Player, Unit, Unit] =
    ScalaComponent
      .builder[Player]
      .render_P(p =>
        <.tbody(
          <.tr(
            <.td(
              StatPaneComponent(p.stats)
            ),
            p.depositories
              .toList
              .sortBy { case (name, _) => name }
              .toTagMod { case (name, depository) =>
                <.td(
                  DepositoryComponent(name, depository)
                )
              },
            <.td(
              EquipmentComponent(p.equipment)
            ),
            <.td(
              <.p(s"Quest points: ${p.questPoints}")
            ),
            <.td(
              LeagueComponent(p.leagueStatus)
            )
          )
        )
      )
      .build
      .apply(player)
}
