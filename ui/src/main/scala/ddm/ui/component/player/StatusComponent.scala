package ddm.ui.component.player

import ddm.ui.component.common.TextBasedTable
import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object StatusComponent {
  def apply(player: Player, itemCache: Map[Item.ID, Item]): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(player, itemCache))

  final case class Props(player: Player, itemCache: Map[Item.ID, Item])

  private def render(props: Props): VdomNode =
    <.table(
      <.tbody(
        <.tr(
          <.td(
            StatPaneComponent(props.player.stats)
          ),
          props
            .player
            .depositories
            .values
            .toList
            .sortBy(_.id.raw)
            .toTagMod(depository =>
              <.td(
                DepositoryComponent(depository, props.itemCache)
              )
            ),
          <.td(
            EquipmentComponent(props.player.equipment, props.itemCache)
          ),
          <.td(
            TextBasedTable(
              "Quest points:" -> props.player.questPoints.toString,
              "Combat level:" -> String.format("%.2f", props.player.stats.combatLevel)
            )
          ),
          <.td(
            LeagueComponent(props.player.leagueStatus)
          )
        )
      )
    )

}
