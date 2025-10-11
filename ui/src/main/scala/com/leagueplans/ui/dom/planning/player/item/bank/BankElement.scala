package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{ContextMenu, Modal}
import com.leagueplans.ui.dom.planning.player.item.{DepositoryStacks, StackElement}
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object BankElement {
  def apply(
    bankSignal: Signal[Depository],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): L.Div =
    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      L.headerTag(
        L.cls(DepositoryStyles.header, PanelStyles.header),
        L.img(L.cls(Styles.icon, DepositoryStyles.icon), L.src(icon), L.alt("Bank icon")),
        "Bank"
      ),
      DepositoryStacks(
        bankSignal.map(cache.itemise),
        columnCount = 8,
        rowCount = 100,
        overflowRowCount = 10,
        toStackElement(bankSignal, effectObserverSignal, contextMenuController, modal)
      ).amend(L.cls(Styles.contents))
    )

  @js.native @JSImport("/images/bank-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/bank/bankElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val contents: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toStackElement(
    bankSignal: Signal[Depository],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  )(stack: ItemStack): L.Div =
    StackElement(stack).amend(
      bindContextMenu(
        stack.item,
        bankSignal,
        effectObserverSignal,
        contextMenuController,
        modal
      )
    )

  private def bindContextMenu(
    item: Item,
    bankSignal: Signal[Depository],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): Binder.Base =
    contextMenuController.bind(menuCloser =>
      Signal
        .combine(bankSignal, effectObserverSignal)
        .map((bank, maybeEffectObserver) =>
          maybeEffectObserver.map { effectObserver =>
            val heldQuantity = bank.contents.getOrElse((item.id, false), 0)
            BankItemContextMenu(item, heldQuantity, effectObserver, menuCloser, modal)
          }
        )
    )
}
