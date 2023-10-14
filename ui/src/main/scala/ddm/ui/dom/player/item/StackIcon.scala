package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, seqToModifier}
import ddm.common.model.Item
import ddm.ui.model.player.item.Stack

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackIcon {
  def apply(stack: Stack, stackSizeSignal: Signal[Int]): L.Div = {
    val itemImage = L.img(
      L.src <-- stackSizeSignal.map(iconPath(stack.item, _)),
      L.alt(s"${stack.item.name} icon")
    )

    L.div(
      L.cls(Styles.icon),
      accountForNoting(itemImage, stack.noted)
    )
  }

  @js.native @JSImport("/images/bank-note.png", JSImport.Default)
  private val note: String = js.native

  @js.native @JSImport("/styles/player/item/stackIcon.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val noted: String = js.native
  }

  private def iconPath(item: Item, stackSize: Int): String =
    s"assets/images/items/${item.imageFor(stackSize).raw}"

  private def accountForNoting(itemImage: L.Image, noted: Boolean): List[L.Image] = {
    if (noted)
      List(
        L.img(L.src(note)),
        itemImage.amend(L.cls(Styles.noted))
      )
    else
      List(itemImage)
  }
}
