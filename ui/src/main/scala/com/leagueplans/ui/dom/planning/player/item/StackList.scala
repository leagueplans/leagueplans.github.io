package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.ui.model.player.item.ItemStack
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

object StackList {
  def apply(
    stacksSignal: Signal[List[ItemStack]],
    toElement: ItemStack => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.children <-- stacksSignal.split(identity)((stack, _, _) =>
        L.li(toElement(stack))
      )
    )
}
