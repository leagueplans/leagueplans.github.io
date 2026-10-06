package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.model.player.item.ItemStack
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

object StackList {
  /** @param stacksSignal the stacks, each with its index in its place */
  def apply(
    stacksSignal: Signal[List[(ItemStack, Int)]],
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      // The index ensures that we can render/remove duplicate stacks (e.g. unnoted, unstackable
      // items in the inventory)
      L.children <-- stacksSignal.split(identity) { case ((stack, _), _, _) =>
        L.li(toElement(stack))
      }
    )
}
