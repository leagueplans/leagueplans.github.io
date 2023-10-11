package ddm.ui.dom.player.item.bank

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, ItemList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, ItemCache}
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object BankElement {
  def apply(
    bankSignal: Signal[Depository],
    itemCache: ItemCache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] =
    ItemList(
      bankSignal.map(itemCache.itemise),
      toItemElement(effectObserverSignal, contextMenuController, modalBus)
    ).amend(L.cls(Styles.bank))

  @js.native @JSImport("/styles/player/item/bank/bankElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val bank: String = js.native
  }

  private def toItemElement(
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  )(item: Item, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(item, stackSizeSignal).amend(
      BankItemContextMenu(
        item,
        stackSizeSignal,
        effectObserverSignal,
        contextMenuController,
        modalBus
      )
    )
}
