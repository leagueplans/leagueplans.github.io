package ddm.ui.component.player

import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
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
    def render(props: Props): VdomNode =
      <.div(
        ^.display.flex,
        Depository.Kind.EquipmentSlot.slots
          .map(props.player.get)
          .toTagMod(DepositoryComponent(_, props.itemCache))
      )
  }
}
