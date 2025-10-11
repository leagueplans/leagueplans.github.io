package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.leagueplans.ui.dom.common.{ContextMenu, Modal}
import com.leagueplans.ui.dom.planning.player.item.{DepositoryStacks, StackElement}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryPanel {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): L.Div =
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      InventoryHeader(),
      DepositoryStacks(
        playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory))),
        columnCount = 4,
        rowCount = 7,
        overflowRowCount = 20,
        toStackElement(playerSignal, cache, effectObserverSignal, contextMenuController, modal)
      ).amend(L.cls(Styles.contents)),
    )

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
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
  
  private def bindItemContextMenu(
    stack: ItemStack,
    cache: Cache,
    playerSignal: Signal[Player],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): Binder.Base =
    contextMenuController.bind(menuCloser =>
      effectObserverSignal.map(_.map(effectObserver =>
        InventoryItemContextMenu(stack, cache, playerSignal, effectObserver, menuCloser, modal)
      ))
    )
}
