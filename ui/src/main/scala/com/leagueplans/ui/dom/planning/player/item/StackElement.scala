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
    * @param customTooltip what the tooltip shows, if not the stack's own details
    */
  def apply(
    stack: ItemStack,
    tooltip: Tooltip,
    tooltipConfig: FloatingConfig,
    hideTooltip: Element => Signal[Boolean],
    customTooltip: Option[L.HtmlElement] = None
  ): L.Div =
    L.div(
      L.cls(Styles.stack),
      StackIcon(stack),
      StackQuantityElement(stack.quantity).amend(L.cls(Styles.stackSize)),
      L.inContext(node =>
        tooltip.register(
          customTooltip.getOrElse(
            tooltipContents(stack.item, Signal.fromValue(Option.when(stack.quantity > 1)(s"Count: ${stack.quantity.withCommas}")))
          ),
          tooltipConfig,
          hideTooltip(node.ref)
        )
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

  /** An item's tooltip: its name, its examine text, and a line about how many there are, if any.
    * Shared with anything else that shows an item, such as the bank search's results. */
  def tooltipContents(item: Item, countLine: Signal[Option[String]]): L.Div =
    L.div(
      L.cls(Styles.tooltip),
      L.p(L.cls(Styles.tooltipHeader), item.name),
      L.when(item.examine.nonEmpty)(L.p(L.cls(Styles.tooltipExamine), item.examine)),
      L.child.maybe <-- countLine.map(_.map(line => L.p(L.cls(Styles.tooltipCount), line)))
    )
}
