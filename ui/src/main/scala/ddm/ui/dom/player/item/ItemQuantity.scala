package ddm.ui.dom.player.item

import com.raquo.laminar.api.{L, textToNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemQuantity {
  def apply(quantity: Int): L.Node =
    quantity match {
      case q if q <= 1 =>
        L.emptyNode
      case q if q < 100000 =>
        L.span(L.cls(Styles.under100K), q.toString)
      case q if q < 10000000 =>
        L.span(L.cls(Styles.under10M), s"${q / 1000}K")
      case q =>
        L.span(L.cls(Styles.over10M), s"${q / 1000000}M")
    }

  @js.native @JSImport("/styles/player/item/itemQuantity.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val under100K: String = js.native
    val under10M: String = js.native
    val over10M: String = js.native
  }
}
