package com.leagueplans.ui.dom.common

import com.leagueplans.ui.utils.laminar.LaminarOps.onKey
import com.raquo.laminar.api.{L, eventPropToProcessor}
import com.raquo.laminar.keys.EventProcessor
import com.raquo.laminar.modifiers.Binder
import org.scalajs.dom.{KeyCode, KeyboardEvent, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Button {
  def apply(
    onClick: EventProcessor[MouseEvent | KeyboardEvent, MouseEvent | KeyboardEvent] => Binder.Base
  ): L.Button =
    L.button(
      L.cls(Styles.button),
      L.`type`("button"),
      onClick(
        eventPropToProcessor(L.onClick)
          .asInstanceOf[EventProcessor[MouseEvent | KeyboardEvent, MouseEvent | KeyboardEvent]]
      ),
      onClick(
        L.onKey(KeyCode.Enter)
          .asInstanceOf[EventProcessor[MouseEvent | KeyboardEvent, MouseEvent | KeyboardEvent]]
      )
    )

  @js.native @JSImport("/styles/common/button.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val button: String = js.native
  }
}
