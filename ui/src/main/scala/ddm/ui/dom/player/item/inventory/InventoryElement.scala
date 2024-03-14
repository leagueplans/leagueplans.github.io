package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.dom.player.item.{StackElement, StackList}
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.item.{Depository, Stack}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.wrappers.fusejs.Fuse

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
  ): L.Div =
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      L.headerTag(
        L.cls(DepositoryStyles.header, PanelStyles.header),
        L.img(L.cls(Styles.icon, DepositoryStyles.icon), L.src(icon), L.alt("Inventory icon")),
        "Inventory"
      ),
      StackList(
        playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory))),
        toStackElement(playerSignal, cache, effectObserverSignal, contextMenuController, modalBus)
      ).amend(L.cls(Styles.contents, DepositoryStyles.contents)),
      bindPanelContextMenu(itemFuse, effectObserverSignal, contextMenuController, modalBus)
    )

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val contents: String = js.native
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
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
        .map((stackSize, maybeEffectObserver) =>
          maybeEffectObserver.map(effectObserver =>
            InventoryItemContextMenu(stack, cache, stackSize, playerSignal, effectObserver, menuCloser, modalBus)
          )
        )
    )
}
