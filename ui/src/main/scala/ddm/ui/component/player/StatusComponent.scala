package ddm.ui.component.player

import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatusComponent {
  def apply(player: Player, itemCache: Map[Item.ID, Item]): Unmounted[Player, Unit, Unit] =
    ScalaComponent
      .builder[Player]
      .render_P(p =>
        <.table(
          <.tbody(
            <.tr(
              <.td(
                StatPaneComponent(p.stats)
              ),
              p.depositories
                .toList
                .sortBy { case (name, _) => name.toString }
                .toTagMod { case (name, depository) =>
                  <.td(
                    DepositoryComponent(name, depository, itemCache)
                  )
                },
              <.td(
                EquipmentComponent(p.equipment, itemCache)
              ),
              <.td(
                <.p(s"Quest points: ${p.questPoints}"),
                <.p(s"Combat level: ${String.format("%.2f", p.stats.combatLevel)}")
              ),
              <.td(
                LeagueComponent(p.leagueStatus)
              )
            )
          )
        )
      )
      .build
      .apply(player)
}
