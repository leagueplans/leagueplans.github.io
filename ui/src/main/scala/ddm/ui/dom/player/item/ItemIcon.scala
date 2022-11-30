package ddm.ui.dom.player.item

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import ddm.common.model.Item

object ItemIcon {
  def apply(item: Item, quantity: Signal[Int]): L.Image =
    L.img(
      L.src <-- quantity.map(iconPath(item, _)),
      L.alt(s"${item.name} icon")
    )

  private def iconPath(item: Item, count: Int): String =
    s"assets/images/items/${item.imageFor(count).raw}"
}
