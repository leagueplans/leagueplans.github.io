package ddm.ui.component.player.item

import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemQuantityComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(quantity: Int, customTags: TagMod = TagMod.empty): Unmounted[Props, Unit, Backend] =
    build(Props(quantity, customTags))

  @js.native @JSImport("/styles/player/item/itemQuantity.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val under100K: String = js.native
    val under10M: String = js.native
    val over10M: String = js.native
  }

  final case class Props(quantity: Int, customTags: TagMod)

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      props.quantity match {
        case q if q <= 1 =>
          EmptyVdom
        case q if q < 100000 =>
          <.span(^.className := s"${Styles.under100K}", q, props.customTags)
        case q if q < 10000000 =>
          <.span(^.className := s"${Styles.under10M}", s"${q / 1000}K", props.customTags)
        case q =>
          <.span(^.className := s"${Styles.over10M}", s"${q / 1000000}M", props.customTags)
      }
  }
}
