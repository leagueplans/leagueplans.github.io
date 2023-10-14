package ddm.ui.dom.player.item.equipment

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.item.Depository
import ddm.ui.model.player.item.Depository.Kind.EquipmentSlot
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

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

    L.button(
      L.`type`("button"),
      "Unequip",
      L.ifUnhandled(L.onClick) -->
        Observer
          .combine(observer, menuCloser)
          .contramap[MouseEvent](_.preventDefault())
    )
  }
}
