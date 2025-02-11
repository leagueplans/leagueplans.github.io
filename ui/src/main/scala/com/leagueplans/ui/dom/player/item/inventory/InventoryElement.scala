package com.leagueplans.ui.dom.player.item.inventory

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub}
import com.leagueplans.ui.dom.player.item.{StackElement, StackList}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.{Depository, Stack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper}
import com.raquo.laminar.modifiers.Binder
import com.raquo.laminar.nodes.ReactiveElement.Base

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val stacks = playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory)))
    
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      InventoryHeader(stacks, modalController, toastPublisher),
      StackList(
        stacks,
        toStackElement(playerSignal, cache, effectObserverSignal, contextMenuController, modalController)
      ).amend(L.cls(Styles.contents, DepositoryStyles.contents)),
      bindPanelContextMenu(itemFuse, effectObserverSignal, contextMenuController, modalController)
    )
  }

  @js.native @JSImport("/styles/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val contents: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
  }

  private def toStackElement(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  )(stack: Stack, stackSizeSignal: Signal[Int]): L.Div =
    StackElement(stack, stackSizeSignal).amend(
      bindItemContextMenu(
        stack,
        cache,
        stackSizeSignal,
        playerSignal,
        effectObserverSignal,
        contextMenuController,
        modalController
      )
    )

  private def bindPanelContextMenu(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalController: Modal.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map(maybeEffectObserver =>
        maybeEffectObserver.map(effectObserver =>
          InventoryContextMenu(itemFuse, effectObserver, menuCloser, modalController)
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
    modalController: Modal.Controller
  ): Binder[Base] =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(stackSizeSignal, effectObserverSignal)
        .map((stackSize, maybeEffectObserver) =>
          maybeEffectObserver.map(effectObserver =>
            InventoryItemContextMenu(stack, cache, stackSize, playerSignal, effectObserver, menuCloser, modalController)
          )
        )
    )
}
