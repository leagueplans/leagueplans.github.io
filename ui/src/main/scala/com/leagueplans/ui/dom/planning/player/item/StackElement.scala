package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackElement {
  def apply(
    stack: ItemStack,
    tooltip: Tooltip,
    tooltipConfig: FloatingConfig
  ): L.Div =
    L.div(
      L.cls(Styles.stack),
      StackIcon(stack),
      StackQuantityElement(stack.quantity).amend(L.cls(Styles.stackSize)),
      tooltip.register(toTooltipContents(stack.item, stack.quantity), tooltipConfig)
    )

  @js.native @JSImport("/styles/planning/player/item/stackElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val stack: String = js.native
    val stackSize: String = js.native
    val tooltip: String = js.native
    val tooltipHeader: String = js.native
    val tooltipExamine: String = js.native
    val tooltipCount: String = js.native
  }

  private def toTooltipContents(item: Item, quantity: Int): L.Div =
    L.div(
      L.cls(Styles.tooltip),
      L.p(L.cls(Styles.tooltipHeader), item.name),
      L.p(L.cls(Styles.tooltipExamine), item.examine),
      L.when(quantity > 1)(
        L.p(
          L.cls(Styles.tooltipCount),
          s"Count: ${String.format("%,d", quantity)}"
        )
      )
    )
}
