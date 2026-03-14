package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.dom.planning.player.item.MoveItemForm
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object BankItemContextMenu {
  def apply(
    item: Item,
    heldQuantity: Int,
    effectObserver: Observer[Effect],
    contextMenu: ContextMenu,
    modal: Modal
  ): L.Div =
    ContextMenuList.from(
      List(
        Some(withdrawButton(item, heldQuantity, note = false, effectObserver, contextMenu, modal)),
        Option.when(item.noteable)(
          withdrawButton(item, heldQuantity, note = true, effectObserver, contextMenu, modal)
        )
      ).flatten
    )

  private def withdrawButton(
    item: Item,
    heldQuantity: Int,
    note: Boolean,
    effectObserver: Observer[MoveItem],
    contextMenu: ContextMenu,
    modal: Modal
  ): ContextMenuList.Item = {
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

    val icon =
      if (note)
        FontAwesome.icon(FreeSolid.faArrowRightToBracket).amend(L.svg.transform("rotate(180)"))
      else
        FontAwesome.icon(FreeSolid.faArrowLeft)

    ContextMenuList.Item(
      icon,
      if (note) "Withdraw noted" else "Withdraw",
      Button(
        _.handled --> Observer.combine(observer, Observer(_ => contextMenu.close()))
      )
    )
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
