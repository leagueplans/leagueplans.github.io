package com.leagueplans.ui.dom.common

import com.raquo.airstream.core.Sink
import com.raquo.laminar.api.L
import com.raquo.laminar.keys.{EventProp, LockedEventKey}
import org.scalajs.dom.MouseEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Button {
  def apply[T](clickSink: Sink[T])(
    convertClicks: EventProp[MouseEvent] => LockedEventKey[MouseEvent, MouseEvent, T]
  ): L.Button =
    L.button(
      L.cls(Styles.button),
      L.`type`("button"),
      convertClicks(L.onClick) --> clickSink
    )

  @js.native @JSImport("/styles/common/button.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val button: String = js.native
  }
}
