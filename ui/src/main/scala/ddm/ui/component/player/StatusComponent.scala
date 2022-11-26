package ddm.ui.component.player

import ddm.ui.component.common.{ContextMenuComponent, DualColumnListComponent}
import ddm.ui.component.player.stats.StatWindowComponent
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object StatusComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val statWindowComponent = StatWindowComponent.build
    private val equipmentComponent = EquipmentComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build
    private val leagueComponent = LeagueComponent.build

    def render(props: Props): VdomNode = {
      <.div(
        <.div(
          ^.display.flex,
          equipmentComponent(EquipmentComponent.Props(
            props.player, props.itemCache
          ))
        ),
        <.div(
          ^.display.flex,
          statWindowComponent(StatWindowComponent.Props(
            props.player.stats,
            props.player.leagueStatus.skillsUnlocked,
            props.addEffectToStep,
            props.contextMenuController
          )),
          DepositoryComponent(props.player.get(Depository.Kind.Inventory), props.itemCache),
          DepositoryComponent(props.player.get(Depository.Kind.Bank), props.itemCache)
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
