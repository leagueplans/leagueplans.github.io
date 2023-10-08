package ddm.ui.dom.player.item

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.ContextMenu
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.Div

object EquipmentElement {
  def apply(
    player: Signal[Player],
    itemCache: ItemCache,
    itemFuse: Fuse[Item],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Div] =
    L.div(
      L.display.flex,
      L.children <-- toSlots(player) { slot =>
        DepositoryElement(slot, itemCache, itemFuse, effectObserver, contextMenuController, modalBus)
      }
    )

  private def toSlots[T](player: Signal[Player])(f: Signal[Depository] => T): Signal[List[T]] =
    player
      .map(p => Depository.Kind.EquipmentSlot.slots.toList.map(slot => p.get(slot)))
      .split(_.kind)((_, _, signal) => f(signal))
}
