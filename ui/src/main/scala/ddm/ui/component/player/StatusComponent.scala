package ddm.ui.component.player

import ddm.ui.component.common.TextBasedTable
import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
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

  private def render(player: Player, itemCache: ItemCache): VdomNode = {
    <.div(
      <.div(
        ^.display.flex,
        StatPaneComponent(player.stats),
        player
          .depositories
          .collect { case (id, depository) if id == Depository.bank.id || id == Depository.inventory.id =>
            depository
          }
          .toList
          .sortBy(_.id.raw)
          .toTagMod(depository =>
            <.td(
              DepositoryComponent.build((depository, itemCache))
            )
          )
      ),
      <.div(
        ^.display.flex,
        EquipmentComponent.build((player.equipment, itemCache))
      ),
      <.div(
        TextBasedTable.build(List(
          "Quest points:" -> player.questPoints.toString,
          "Combat level:" -> String.format("%.2f", player.stats.combatLevel)
        )),
        LeagueComponent.build(player.leagueStatus)
      )
    )
  }
}
