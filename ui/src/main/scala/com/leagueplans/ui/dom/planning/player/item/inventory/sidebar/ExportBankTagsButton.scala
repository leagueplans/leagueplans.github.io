package com.leagueplans.ui.dom.planning.player.item.inventory.sidebar

import com.leagueplans.ui.dom.common.{Button, Modal, ToastHub}
import com.leagueplans.ui.dom.planning.player.item.inventory.forms.ExportBankTagsForm
import com.leagueplans.ui.model.player.item.ItemStack
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

object ExportBankTagsButton {
  def apply(
    stacksSignal: Signal[List[ItemStack]],
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Button = {
    val form = ExportBankTagsForm(stacksSignal, toastPublisher)
    Button(
      _.handled --> (_ => modal.show(form))
    ).amend(
      "Export tags",
      L.disabled <-- stacksSignal.map(_.isEmpty)
    )
  }
}
