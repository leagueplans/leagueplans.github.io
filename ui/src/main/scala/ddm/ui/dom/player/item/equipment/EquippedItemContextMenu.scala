package ddm.ui.dom.player.item.equipment

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common.{Button, ContextMenu}
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.utils.laminar.LaminarOps.handled

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

    Button(
      Observer.combine(observer, menuCloser)
    )(_.handled).amend("Unequip")
  }
}
