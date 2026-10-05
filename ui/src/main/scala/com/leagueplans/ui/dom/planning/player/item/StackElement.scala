package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StackElement {
  /** @param hideTooltip whether to hide the stack's tooltip, given the stack's element, such as
    *                    while the stack's card is open
    */
  def apply(
    stack: ItemStack,
    tooltip: Tooltip,
    tooltipConfig: FloatingConfig,
    hideTooltip: Element => Signal[Boolean]
  ): L.Div =
    L.div(
      L.cls(Styles.stack),
      StackIcon(stack),
      StackQuantityElement(stack.quantity).amend(L.cls(Styles.stackSize)),
      L.inContext(node =>
        tooltip.register(toTooltipContents(stack.item, stack.quantity), tooltipConfig, hideTooltip(node.ref))
      )
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
          s"Count: ${quantity.withCommas}"
        )
      )
    )
}
