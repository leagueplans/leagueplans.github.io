package ddm.ui.component.player.item

import ddm.common.model.Item
import ddm.ui.component.common.ContextMenuComponent
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object EquipmentComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(
    player: Player,
    itemCache: ItemCache,
    items: Fuse[Item],
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  ): Unmounted[Props, Unit, Backend] =
    build(Props(player, itemCache, items, addEffectToStep, contextMenuController))

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    items: Fuse[Item],
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.div(
        ^.display.flex,
        Depository.Kind.EquipmentSlot.slots
          .map(props.player.get)
          .toTagMod(DepositoryComponent(
            _,
            props.itemCache,
            props.items,
            props.addEffectToStep,
            props.contextMenuController
          ))
      )
  }
}
