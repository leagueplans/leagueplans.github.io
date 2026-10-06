package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.model.plan.ItemQuantity
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.KeyValue

/** Steps a text box's item count up or down with the arrow keys, as a number box does. "max", and
  * text that isn't a count, leave the keys to move the caret. */
object AmountStepKeys {
  /** @param onText told about the box's new text, which the box already shows */
  def apply(onText: Observer[String] = Observer.empty): L.Modifier[L.Input] =
    L.inContext(node =>
      L.onKeyDown --> { event =>
        val by = event.key match {
          case KeyValue.ArrowUp => 1
          case KeyValue.ArrowDown => -1
          case _ => 0
        }
        if (by != 0) ItemQuantity.stepped(node.ref.value, by).foreach { text =>
          event.preventDefault()
          node.ref.value = text
          onText.onNext(text)
        }
      }
    )
}
