package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, Stack}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[OList] =
    StackList(
      playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory))),
      toStackElement(playerSignal, cache, effectObserverSignal, contextMenuController, modalBus)
    ).amend(
      L.cls(Styles.inventory),
      bindPanelContextMenu(itemFuse, effectObserverSignal, contextMenuController, modalBus)
    )

  @js.native @JSImport("/styles/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val inventory: String = js.native
  }

  private def toStackElement(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  )(stack: Stack, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(stack, stackSizeSignal).amend(
      bindItemContextMenu(
        stack,
        cache,
        stackSizeSignal,
        playerSignal,
        effectObserverSignal,
        contextMenuController,
        modalBus
      )
    )

  private def bindPanelContextMenu(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map(maybeEffectObserver =>
        maybeEffectObserver.map(effectObserver =>
          InventoryContextMenu(itemFuse, effectObserver, menuCloser, modalBus)
        )
      )
    )

  private def bindItemContextMenu(
    stack: Stack,
    cache: Cache,
    stackSizeSignal: Signal[Int],
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(stackSizeSignal, effectObserverSignal)
        .map { case (stackSize, maybeEffectObserver) =>
          maybeEffectObserver.map(effectObserver =>
            InventoryItemContextMenu(stack, cache, stackSize, playerSignal, effectObserver, menuCloser, modalBus)
          )
        }
    )
}
