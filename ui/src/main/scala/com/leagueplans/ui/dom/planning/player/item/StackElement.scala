package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{KeyValuePairs, Tooltip}
import com.leagueplans.ui.model.player.item.ItemStack
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackElement {
  def apply(stack: ItemStack): L.Div =
    L.div(
      L.cls(Styles.stack),
      StackIcon(stack),
      StackQuantityElement(stack.quantity).amend(L.cls(Styles.stackSize)),
      tooltip(stack.item, stack.quantity)
    )

  @js.native @JSImport("/styles/planning/player/item/stackElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stack: String = js.native
    val stackSize: String = js.native
  }

  private def tooltip(item: Item, quantity: Int): L.Modifier[L.HtmlElement] =
    Tooltip(KeyValuePairs(
      L.span("Name:") -> L.span(item.name),
      L.span("Examine:") -> L.span(item.examine),
      L.span("Quantity:") -> L.span(quantity)
    ))
}
