package ddm.ui.component.player

import ddm.common.model.Item
import ddm.ui.ResourcePaths
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ItemIconComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(item: Item, count: Int)

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.img(
        ^.src := ResourcePaths.itemIcon(props.item, props.count),
        ^.alt := s"${props.item.name} icon"
      )
  }
}
