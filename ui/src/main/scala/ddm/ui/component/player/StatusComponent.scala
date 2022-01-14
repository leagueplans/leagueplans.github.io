package ddm.ui.component.player

import ddm.ui.component.common.TextBasedTable
import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object StatusComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (Player, ItemCache)

  private def render(player: Player, itemCache: ItemCache): VdomNode =
    <.table(
      <.tbody(
        <.tr(
          <.td(
            StatPaneComponent(player.stats)
          ),
          player
            .depositories
            .values
            .toList
            .sortBy(_.id.raw)
            .toTagMod(depository =>
              <.td(
                DepositoryComponent.build((depository, itemCache))
              )
            ),
          <.td(
            EquipmentComponent.build((player.equipment, itemCache))
          ),
          <.td(
            TextBasedTable.build(List(
              "Quest points:" -> player.questPoints.toString,
              "Combat level:" -> String.format("%.2f", player.stats.combatLevel)
            ))
          ),
          <.td(
            LeagueComponent.build(player.leagueStatus)
          )
        )
      )
    )

}
