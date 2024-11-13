package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.common.model.Item
import ddm.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import ddm.ui.model.plan.Effect.AddItem
import ddm.ui.model.player.item.Depository
import ddm.ui.utils.laminar.LaminarOps.handled
import ddm.ui.wrappers.fusejs.Fuse

object InventoryContextMenu {
  def apply(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modalController: Modal.Controller
  ): L.Button =
    toElement(
      toAddItemFormOpener(itemFuse, effectObserver, modalController),
      menuCloser
    )

  private def toElement(
    addItemFormOpener: Observer[FormOpener.Command],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(
      Observer.combine(addItemFormOpener, menuCloser)
    )(_.handled).amend("Add item")

  private def toAddItemFormOpener(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    modalController: Modal.Controller
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalController,
      effectObserver,
      () => {
        val (form, formSubmissions) = AddItemForm(Depository.Kind.Inventory, itemFuse, modalController)
        (form, formSubmissions.collect { case Some(effect) => effect })
      }
    )
}
