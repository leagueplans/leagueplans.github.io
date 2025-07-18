package com.leagueplans.ui.dom.planning.player.item

import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackQuantityElement {
  def apply(quantity: Int): L.Span = {
    val clsAndRepr = classAndRepr(quantity)
    L.span(L.cls(clsAndRepr.cls), clsAndRepr.repr)
  }

  @js.native @JSImport("/styles/planning/player/item/stackQuantityElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val under100K: String = js.native
    val under10M: String = js.native
    val over10M: String = js.native
  }

  private def classAndRepr(quantity: Int): (cls: String, repr: String) =
    quantity match {
      case n if n == 1 =>
        (Styles.under100K, "")
      case n if n < 100000 =>
        (Styles.under100K, n.toString)
      case n if n < 10000000 =>
        (Styles.under10M, s"${n / 1000}K")
      case n =>
        (Styles.over10M, s"${n / 1000000}M")
    }
}
