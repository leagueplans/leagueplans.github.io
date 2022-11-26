package ddm.ui.component.player

import ddm.common.model.Item
import ddm.ui.model.player.item.Depository.Kind
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DepositoryComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(depository: Depository, itemCache: ItemCache): Unmounted[Props, Unit, Backend] =
    build(Props(depository, itemCache))

  @js.native @JSImport("/styles/player/depository.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
    val bank: String = js.native
    val equipmentSlot: String = js.native
  }

  final case class Props(depository: Depository, itemCache: ItemCache) {
    val contents: List[(Item, Int)] =
      itemCache.itemise(depository)
  }

  final class Backend(scope: BackendScope[Props, Unit]) {
    def render(props: Props): VdomNode =
      <.ol(
        ^.className := style(props.depository.kind),
        props.contents.map { case (item, quantity) =>
          <.li(ItemComponent(item, quantity))
        }.toTagMod
      )

    private def style(depository: Depository.Kind): String =
      depository match {
        case Kind.Inventory => Styles.inventory
        case Kind.Bank => Styles.bank
        case _: Kind.EquipmentSlot => Styles.equipmentSlot
      }
  }
}
