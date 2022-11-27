package ddm.ui.component.player

import ddm.common.model.Item
import ddm.ui.component.common.{ContextMenuComponent, DualColumnListComponent}
import ddm.ui.component.player.item.{DepositoryComponent, EquipmentComponent}
import ddm.ui.component.player.stats.StatWindowComponent
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
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
    items: Fuse[Item],
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val statWindowComponent = StatWindowComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build
    private val leagueComponent = LeagueComponent.build

    def render(props: Props): VdomNode = {
      <.div(
        <.div(
          ^.display.flex,
          EquipmentComponent(
            props.player,
            props.itemCache,
            props.items,
            props.addEffectToStep,
            props.contextMenuController
          )
        ),
        <.div(
          ^.display.flex,
          statWindowComponent(StatWindowComponent.Props(
            props.player.stats,
            props.player.leagueStatus.skillsUnlocked,
            props.addEffectToStep,
            props.contextMenuController
          )),
          List(Depository.Kind.Inventory, Depository.Kind.Bank).toTagMod(kind =>
            DepositoryComponent(
              props.player.get(kind),
              props.itemCache,
              props.items,
              props.addEffectToStep,
              props.contextMenuController
            )
          )
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
