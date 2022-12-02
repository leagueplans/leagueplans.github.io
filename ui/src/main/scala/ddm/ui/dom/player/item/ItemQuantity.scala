package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper, textToNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ItemQuantity {
  def apply(quantity: Signal[Int]): L.Span = {
    val clsAndRepr = quantity.map(classAndRepr)
    L.span(
      L.cls <-- clsAndRepr.map(_._1),
      L.child <-- clsAndRepr.map(_._2)
    )
  }

  @js.native @JSImport("/styles/player/item/itemQuantity.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val under100K: String = js.native
    val under10M: String = js.native
    val over10M: String = js.native
  }

  private def classAndRepr(quantity: Int): (String, String) =
    quantity match {
      case q if q == 1 =>
        (Styles.under100K, "")
      case q if q < 100000 =>
        (Styles.under100K, q.toString)
      case q if q < 10000000 =>
        (Styles.under10M, s"${q / 1000}K")
      case q =>
        (Styles.over10M, s"${q / 1000000}M")
    }
}
