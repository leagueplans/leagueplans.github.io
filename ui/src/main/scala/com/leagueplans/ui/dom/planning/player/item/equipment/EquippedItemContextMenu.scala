package com.leagueplans.ui.dom.planning.player.item.equipment

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{Button, ContextMenu}
import com.leagueplans.ui.model.plan.Effect.MoveItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

object EquippedItemContextMenu {
  def apply(
    item: Item,
    stackSize: Int,
    slot: EquipmentSlot,
    effectObserver: Observer[MoveItem],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button = {
    val observer = effectObserver.contramap[Unit](_ =>
      MoveItem(
        item.id,
        stackSize,
        slot,
        notedInSource = false,
        Depository.Kind.Inventory,
        noteInTarget = false
      )
    )

    Button(_.handled --> Observer.combine(observer, menuCloser)).amend("Unequip")
  }
}
