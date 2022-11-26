package ddm.ui.component.player

import ddm.common.model.Item
import ddm.ui.component.common.{DualColumnListComponent, ElementWithTooltipComponent}
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(item: Item, quantity: Int): Unmounted[Props, Unit, Backend] =
    build(Props(item, quantity))

  @js.native @JSImport("/styles/player/item.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val quantity: String = js.native
  }

  final case class Props(item: Item, quantity: Int)

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val elementWithTooltipComponent = ElementWithTooltipComponent.build
    private val dualColumnListComponent = DualColumnListComponent.build

    def render(props: Props): VdomNode =
      elementWithTooltipComponent(ElementWithTooltipComponent.Props(
        renderItem(props, _),
        renderTooltip(props, _)
      ))

    private def renderItem(props: Props, tooltipTags: TagMod): VdomNode =
      <.div(
        tooltipTags,
        ItemQuantityComponent(props.quantity, ^.className := Styles.quantity),
        ItemIconComponent(props.item, props.quantity),
      )

    private def renderTooltip(props: Props, tooltipTags: TagMod): VdomNode =
      <.div(
        tooltipTags,
        dualColumnListComponent(List(
          ("Name:", props.item.name),
          ("ID prefix:", props.item.id.raw.take(8)),
          ("Quantity:", props.quantity)
        ))
      )
  }
}
