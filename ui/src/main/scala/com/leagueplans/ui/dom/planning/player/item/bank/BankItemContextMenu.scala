package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.player.item.MoveItemForm
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, optionToModifier, textToTextNode}

object BankItemContextMenu {
  def apply(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[Effect],
    controller: ContextMenu.Controller,
    modal: Modal
  ): L.Div =
    L.div(
      withdrawButton(item, heldQuantity, note = false, effectObserver, controller, modal),
      Option.when(item.noteable)(
        withdrawButton(item, heldQuantity, note = true, effectObserver, controller, modal)
      )
    )

  private def withdrawButton(
    item: Item,
    heldQuantity: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    controller: ContextMenu.Controller,
    modal: Modal
  ): L.Node = {
    val observer =
      if (heldQuantity > 1)
        toWithdrawItemFormOpener(item, heldQuantity, note, effectObserver, modal)
      else
        effectObserver.contramap[Unit](_ =>
          MoveItem(
            item.id,
            heldQuantity,
            Depository.Kind.Bank,
            notedInSource = false,
            Depository.Kind.Inventory,
            noteInTarget = note
          )
        )

    Button(
      _.handled --> Observer.combine(observer, Observer(_ => controller.close()))
    ).amend(if (note) "Withdraw noted" else "Withdraw")
  }

  private def toWithdrawItemFormOpener(
    item: Item,
    heldQuantity: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    modal: Modal
  ): Observer[Any] =
    FormOpener(
      modal: Modal,
      MoveItemForm(
        ItemStack(item, noted = false, heldQuantity),
        Depository.Kind.Bank,
        Depository.Kind.Inventory,
        note
      ),
      effectObserver.contracollect[Option[MoveItem]] { case Some(effect) => effect }
    ).toObserver
}
