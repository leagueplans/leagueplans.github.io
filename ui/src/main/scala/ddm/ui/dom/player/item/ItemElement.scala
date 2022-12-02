package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.{KeyValuePairs, Tooltip}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemElement {
  def apply(item: Item, quantity: Signal[Int]): L.Div =
    L.div(
      ItemQuantity(quantity).amend(L.cls(Styles.quantity)),
      ItemIcon(item, quantity),
      tooltip(item, quantity)
    )

  @js.native @JSImport("/styles/player/item/item.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val quantity: String = js.native
  }

  private def tooltip(item: Item, quantity: Signal[Int]): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("Name:") -> L.span(item.name),
      L.span("ID prefix:") -> L.span(item.id.raw.take(8)),
      L.span("Quantity:") -> L.span(L.child <-- quantity.map(_.toString))
    ))
}
