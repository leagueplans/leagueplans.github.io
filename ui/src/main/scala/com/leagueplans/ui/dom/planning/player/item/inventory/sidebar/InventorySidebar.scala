package com.leagueplans.ui.dom.planning.player.item.inventory.sidebar

import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{Modal, ToastHub}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventorySidebar {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[Effect | Seq[Effect]]]],
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): L.Div = {
    val bankAllButton = BankAllButton(effectObserverSignal)

    val bankTagsButton =
      ExportBankTagsButton(
        playerSignal.map(player => cache.itemise(player.get(Depository.Kind.Inventory))),
        modal,
        toastPublisher
      )

    L.div(
      L.cls(Styles.sidebar),
      bankAllButton.amend(L.cls(Styles.bankAllButton)),
      bankTagsButton.amend(L.cls(Styles.exportBankTagsButton))
    )
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/sidebar/inventorySidebar.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val sidebar: String = js.native
    val bankAllButton: String = js.native
    val exportBankTagsButton: String = js.native
  }
}
