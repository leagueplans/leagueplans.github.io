package ddm.ui.component.player

import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Equipment, ItemCache}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object EquipmentComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(player: Player, itemCache: ItemCache)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val depositoryComponent = DepositoryComponent.build

    def render(props: Props): VdomNode =
      <.div(
        ^.display.flex,
        Equipment
          .initial
          .raw
          .keys
          .map(props.player.depositories)
          .toList
          .sortBy(_.id.raw)
          .toTagMod(d => depositoryComponent(DepositoryComponent.Props(d, props.itemCache)))
      )
  }
}
