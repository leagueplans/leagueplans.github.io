package com.leagueplans.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackSizeElement {
  def apply(stackSizeSignal: Signal[Int]): L.Span = {
    val clsAndRepr = stackSizeSignal.map(classAndRepr)
    L.span(
      L.cls <-- clsAndRepr.map(_._1),
      L.child <-- clsAndRepr.map(_._2)
    )
  }

  @js.native @JSImport("/styles/player/item/stackSizeElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val under100K: String = js.native
    val under10M: String = js.native
    val over10M: String = js.native
  }

  private def classAndRepr(stackSize: Int): (String, String) =
    stackSize match {
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
