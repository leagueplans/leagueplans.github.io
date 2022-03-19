package ddm.ui.component.common

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ElementWithTooltipComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    renderElement: TagMod => VdomNode,
    renderTooltip: TagMod => VdomNode
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      props.renderElement(TagMod(
        ^.className := "element-with-tooltip",
        props.renderTooltip(^.className := "tooltip")
      ))
  }
}
