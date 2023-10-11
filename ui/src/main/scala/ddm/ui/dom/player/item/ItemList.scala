package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import org.scalajs.dom.html.OList

object ItemList {
  /** @param stacksSignal
    *   a list consisting of unique items, each paired with a second list
    *   describing the size of distinct stacks of the item. For example,
    *   five hammers in the inventory would be represented as
    *   List(Hammer -> List(1, 1, 1, 1, 1))
    *   The same five hammers in the bank would be represented as
    *   List(Hammer -> List(5))
    * @param toElement
    *   a function which creates the HTML element associated with an item
    *   stack and a signal containing the current size of the stack
    */
  def apply(
    stacksSignal: Signal[List[(Item, List[Int])]],
    toElement: (Item, Signal[Int]) => L.Modifier[L.HtmlElement]
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.children <--
        stacksSignal
          .map(mixedStacks =>
            mixedStacks.flatMap { case (item, stacks) =>
              stacks.zipWithIndex.map { case (stackSize, stackIndex) =>
                (item, stackSize, stackIndex)
              }
            }
          )
          .split { case (item, _, stackIndex) => (item, stackIndex) } { case ((item, _), _, signal) =>
            L.li(toElement(item, signal.map { case (_, stackSize, _) => stackSize }))
          }
    )
}
