package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object ElementWithTooltipComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P((render _).tupled)
      .build

  type Props = (VdomNode, VdomNode)

  private def render(element: VdomNode, tooltip: VdomNode): VdomNode =
    <.div(
      ^.className := "element-with-tooltip",
      element,
      <.div(
        ^.className := "tooltip",
        tooltip
      )
    )
}
