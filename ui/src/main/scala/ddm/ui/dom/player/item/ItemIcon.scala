package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import ddm.common.model.Item

object ItemIcon {
  def apply(item: Item, stackSizeSignal: Signal[Int]): L.Image =
    L.img(
      L.src <-- stackSizeSignal.map(iconPath(item, _)),
      L.alt(s"${item.name} icon")
    )

  private def iconPath(item: Item, stackSize: Int): String =
    s"assets/images/items/${item.imageFor(stackSize).raw}"
}
