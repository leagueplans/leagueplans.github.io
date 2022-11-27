package ddm.ui.component.player.item

import ddm.common.model.Item
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object ItemIconComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(item: Item, quantity: Int, customTags: TagMod = TagMod.empty): Unmounted[Props, Unit, Backend] =
    build(Props(item, quantity, customTags))

  final case class Props(item: Item, quantity: Int, customTags: TagMod)

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.img(
        ^.src := iconPath(props.item, props.quantity),
        ^.alt := s"${props.item.name} icon",
        props.customTags
      )
  }

  private def iconPath(item: Item, count: Int): String =
    s"assets/images/items/${item.imageFor(count).raw}"
}
