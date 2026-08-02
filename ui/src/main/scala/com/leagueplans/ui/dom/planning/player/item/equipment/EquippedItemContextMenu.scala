package com.leagueplans.ui.dom.planning.player.item.equipment

import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.L

object EquippedItemContextMenu {
  def apply(
    stack: ItemStack,
    slot: EquipmentSlot,
    effectObserver: Observer[MoveItem],
    contextMenu: ContextMenu
  ): L.Div = {
    val observer = effectObserver.contramap[Unit](_ =>
      MoveItem(
        stack.item.id,
        stack.quantity,
        slot,
        notedInSource = stack.noted,
        Depository.Kind.Inventory,
        noteInTarget = stack.noted
      )
    )

    ContextMenuList(
      ContextMenuList.Item(
        FontAwesome.icon(FreeSolid.faBriefcase),
        "Unequip",
        Button(
          _.handled --> Observer.combine(observer, Observer(_ => contextMenu.close()))
        )
      )
    )
  }
}
