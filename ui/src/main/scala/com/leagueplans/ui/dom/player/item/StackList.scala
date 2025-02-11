package com.leagueplans.ui.dom.player.item

import com.leagueplans.ui.model.player.item.Stack
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

object StackList {
  /** @param stacksSignal
    *   a list consisting of unique items, each paired with a second list
    *   describing the size of distinct stacks of the item. For example,
    *   five hammers in the inventory would be represented as
    *   List((Hammer, unnoted) -> List(1, 1, 1, 1, 1))
    *   The same five hammers in the bank would be represented as
    *   List((Hammer, unnoted) -> List(5))
    */
  def apply(
    stacksSignal: Signal[List[(Stack, List[Int])]],
    toElement: (Stack, Signal[Int]) => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.children <--
        stacksSignal
          .map(mixedStacks =>
            mixedStacks.flatMap((item, stacks) =>
              stacks.zipWithIndex.map((stackSize, stackIndex) =>
                (item, stackSize, stackIndex)
              )
            )
          )
          .split((item, _, stackIndex) => (item, stackIndex)) { case ((item, _), _, signal) =>
            L.li(toElement(item, signal.map((_, stackSize, _) => stackSize)))
          }
    )
}
