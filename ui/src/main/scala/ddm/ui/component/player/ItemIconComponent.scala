package ddm.ui.component.player

import ddm.ui.ResourcePaths
import ddm.ui.model.player.item.Item
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object ItemIconComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Props = Item

  private def render(item: Item): VdomNode =
    <.img(
      ^.src := ResourcePaths.itemIcon(item.id),
      ^.alt := s"${item.name} icon"
    )
}
