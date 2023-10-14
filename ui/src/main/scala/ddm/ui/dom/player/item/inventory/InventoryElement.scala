package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache, Stack}
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] =
    StackList(
      playerSignal.map(player => itemCache.itemise(player.get(Depository.Kind.Inventory))),
      toStackElement(playerSignal, itemCache, effectObserverSignal, contextMenuController, modalBus)
    ).amend(
      L.cls(Styles.inventory),
      InventoryContextMenu(itemFuse, effectObserverSignal, contextMenuController, modalBus)
    )

  @js.native @JSImport("/styles/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
  }

  private def toStackElement(
    playerSignal: Signal[Player],
    itemCache: ItemCache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  )(stack: Stack, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(stack, stackSizeSignal).amend(
      InventoryItemContextMenu(
        stack,
        itemCache,
        stackSizeSignal,
        playerSignal,
        effectObserverSignal,
        contextMenuController,
        modalBus
      )
    )
}
