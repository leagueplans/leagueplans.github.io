package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub}
import com.leagueplans.ui.dom.planning.player.item.{StackElement, StackList}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryElement {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val stacks = playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory)))
    
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      InventoryHeader(stacks, modal, toastPublisher),
      StackList(
        stacks,
        toStackElement(playerSignal, cache, effectObserverSignal, contextMenuController, modal)
      ).amend(L.cls(Styles.contents, DepositoryStyles.contents)),
      bindPanelContextMenu(itemFuse, effectObserverSignal, contextMenuController, modal)
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/inventoryElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val contents: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
  }

  private def toStackElement(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  )(stack: ItemStack): L.Div =
    StackElement(stack).amend(
      bindItemContextMenu(
        stack,
        cache,
        playerSignal,
        effectObserverSignal,
        contextMenuController,
        modal
      )
    )

  private def bindPanelContextMenu(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): Binder[L.Element] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map(maybeEffectObserver =>
        maybeEffectObserver.map(effectObserver =>
          InventoryContextMenu(itemFuse, effectObserver, menuCloser, modal)
        )
      )
    )

  private def bindItemContextMenu(
    stack: ItemStack,
    cache: Cache,
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): Binder[L.Element] =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map(_.map(effectObserver =>
        InventoryItemContextMenu(stack, cache, playerSignal, effectObserver, menuCloser, modal)
      ))
    )
}
