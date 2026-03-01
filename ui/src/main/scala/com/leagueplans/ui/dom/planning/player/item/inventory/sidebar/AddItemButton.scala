package com.leagueplans.ui.dom.planning.player.item.inventory.sidebar

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{Button, FormOpener, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.player.item.inventory.forms.AddItemForm
import com.leagueplans.ui.model.plan.Effect.AddItem
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

object AddItemButton {
  def apply(
    itemFuse: Fuse[Item],
    effectObserverSignal: Signal[Option[Observer[AddItem]]],
    tooltip: Tooltip,
    modal: Modal
  ): L.Button =
    Button(
      _.handledWith(
        _.sample(effectObserverSignal).collectSome
      ) --> createClickObserver(itemFuse, tooltip, modal)
    ).amend(
      "Add items",
      L.disabled <-- effectObserverSignal.map(_.isEmpty)
    )

  private def createClickObserver(
    itemFuse: Fuse[Item],
    tooltip: Tooltip,
    modal: Modal
  ): Observer[Observer[AddItem]] =
    Observer(effectObserver =>
      FormOpener(
        modal,
        AddItemForm(Depository.Kind.Inventory, itemFuse, tooltip, modal),
        effectObserver.contracollect[Option[AddItem]] { case Some(effect) => effect }
      ).open()
    )
}
