package com.leagueplans.ui.dom.planning.player.item.inventory

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{Button, ContextMenu, FormOpener, Modal}
import com.leagueplans.ui.model.plan.Effect.AddItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}

object InventoryContextMenu {
  def apply(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    menuCloser: Observer[ContextMenu.CloseCommand],
    modal: Modal
  ): L.Button =
    toElement(toAddItemFormOpener(itemFuse, effectObserver, modal), menuCloser)

  private def toElement(
    addItemFormOpener: FormOpener,
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    Button(_.handled --> Observer.combine(
      addItemFormOpener.toObserver,
      menuCloser
    )).amend("Add item")

  private def toAddItemFormOpener(
    itemFuse: Fuse[Item],
    effectObserver: Observer[AddItem],
    modal: Modal
  ): FormOpener =
    FormOpener(
      modal,
      AddItemForm(Depository.Kind.Inventory, itemFuse, modal),
      effectObserver.contracollect[Option[AddItem]] { case Some(effect) => effect }
    )
}
