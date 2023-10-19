package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common.{ContextMenu, FormOpener}
import ddm.ui.model.plan.Effect.AddItem
import ddm.ui.model.player.item.Depository
import ddm.ui.utils.laminar.LaminarOps.RichEventProp
import ddm.ui.wrappers.fusejs.Fuse

object InventoryContextMenu {
  def apply(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalBus: WriteBus[Option[L.Element]]
  ): L.Button =
    toElement(
      toAddItemFormOpener(itemFuse, effectObserver, modalBus),
      menuCloser
    )

  private def toAddItemFormOpener(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    modalBus: WriteBus[Option[L.Element]]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = AddItemForm(Depository.Kind.Inventory, itemFuse, modalBus)
    FormOpener(
      modalBus,
      effectObserver,
      () => (form, formSubmissions.collect { case Some(effect) => effect })
    )
  }

  private def toElement(
    addItemFormOpener: Observer[FormOpener.Command],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    L.button(
      L.`type`("button"),
      "Add item",
      L.onClick.handled --> Observer.combine(addItemFormOpener, menuCloser)
    )
}
