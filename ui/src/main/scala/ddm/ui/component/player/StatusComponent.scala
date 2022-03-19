package ddm.ui.component.player

import ddm.ui.component.common.DualColumnListComponent
import ddm.ui.component.player.stats.StatPaneComponent
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object StatusComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(player: Player, itemCache: ItemCache)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val statPaneComponent = StatPaneComponent.build
    private val depositoryComponent = DepositoryComponent.build
    private val equipmentComponent = EquipmentComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build
    private val leagueComponent = LeagueComponent.build

    def render(props: Props): VdomNode = {
      <.div(
        <.div(
          ^.display.flex,
          statPaneComponent(StatPaneComponent.Props(
            props.player.stats,
            props.player.leagueStatus.skillsUnlocked
          )),
          props.player
            .depositories
            .collect { case (id, depository) if id == Depository.bank.id || id == Depository.inventory.id =>
              depository
            }
            .toList
            .sortBy(_.id.raw)
            .toTagMod(depository =>
              <.td(
                depositoryComponent(DepositoryComponent.Props(depository, props.itemCache))
              )
            )
        ),
        <.div(
          ^.display.flex,
          equipmentComponent(EquipmentComponent.Props(
            props.player, props.itemCache
          ))
        ),
        <.div(
          dualColumnListComponent(List(
            ("Quest points:", props.player.questPoints),
            ("Combat level:", String.format("%.2f", props.player.stats.combatLevel))
          )),
          leagueComponent(props.player.leagueStatus)
        )
      )
    }
  }
}
