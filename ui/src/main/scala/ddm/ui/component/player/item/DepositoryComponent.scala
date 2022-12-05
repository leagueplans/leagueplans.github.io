package ddm.ui.component.player.item

import ddm.common.model.Item
import ddm.ui.component.common.{ContextMenuComponent, ModalComponent}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.Depository.Kind
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, Ref, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DepositoryComponent {
  private val build: Component[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(
    depository: Depository,
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  ): Unmounted[Props, Unit, Backend] =
    build(Props(depository, itemCache, itemFuse, addEffectToStep, contextMenuController))

  @js.native @JSImport("/styles/player/item/depository.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
    val bank: String = js.native
    val equipmentSlot: String = js.native
  }

  final case class Props(
    depository: Depository,
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    addEffectToStep: Option[Effect => Callback],
    contextMenuController: ContextMenuComponent.Controller
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val modalComponent = ModalComponent.build(ModalComponent.State.Hidden)
    private val modalComponentRef = Ref.toScalaComponent(modalComponent)
    private val modalController = new ModalComponent.Controller(modalComponentRef)

    def render(props: Props): VdomNode =
      ReactFragment(
        modalComponent.withRef(modalComponentRef)(),
        renderContents(props)
      )

    private def renderContents(props: Props): VdomNode =
      <.ol(
        ^.className := style(props.depository.kind),
        props.itemCache
          .itemise(props.depository)
          .flatMap { case (item, stacks) => stacks.map(item -> _) }
          .toTagMod { case (item, quantity) => <.li(ItemComponent(item, quantity)) },
        renderContextMenu(
          props.depository.kind,
          props.itemFuse,
          props.addEffectToStep,
          props.contextMenuController
        )
      )

    private def style(depository: Depository.Kind): String =
      depository match {
        case Kind.Inventory => Styles.inventory
        case Kind.Bank => Styles.bank
        case _: Kind.EquipmentSlot => Styles.equipmentSlot
      }

    private def renderContextMenu(
      target: Depository.Kind,
      itemFuse: Fuse[Item],
      addEffectToStep: Option[Effect => Callback],
      contextMenuController: ContextMenuComponent.Controller
    ): TagMod =
      addEffectToStep.toTagMod(addEffect =>
        contextMenuController.show(
          <.span(
            "Gain item",
            ^.onClick --> modalController.show(
              GainItemComponent(
                target,
                itemFuse,
                Callback.traverseOption(_)(addEffect(_)) *> modalController.hide()
              )
            ) *> contextMenuController.hide()
          )
        )
      )
  }
}
