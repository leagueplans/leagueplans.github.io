package ddm.ui.component.common

import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._

object ElementWithTooltipComponent {
  def apply(element: VdomNode, tooltip: VdomNode): Unmounted[Props, Unit, Unit] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build
      .apply(Props(element, tooltip))

  final case class Props(element: VdomNode, contents: VdomNode)

  private def render(props: Props): VdomNode =
    <.div(
      ^.className := "element-with-tooltip",
      props.element,
      <.div(
        ^.className := "tooltip",
        props.contents
      )
    )
}
