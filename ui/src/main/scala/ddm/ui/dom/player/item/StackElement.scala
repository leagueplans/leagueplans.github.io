package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, intToNode, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.{KeyValuePairs, Tooltip}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackElement {
  def apply(item: Item, sizeSignal: Signal[Int]): L.Div =
    L.div(
      L.cls(Styles.item),
      StackSizeElement(sizeSignal).amend(L.cls(Styles.stackSize)),
      ItemIcon(item, sizeSignal),
      tooltip(item, sizeSignal)
    )

  @js.native @JSImport("/styles/player/item/stackElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val item: String = js.native
    val stackSize: String = js.native
  }

  private def tooltip(item: Item, sizeSignal: Signal[Int]): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("Name:") -> L.span(item.name),
      L.span("ID prefix:") -> L.span(item.id.raw.take(8)),
      L.span("Quantity:") -> L.span(L.child.text <-- sizeSignal)
    ))
}
