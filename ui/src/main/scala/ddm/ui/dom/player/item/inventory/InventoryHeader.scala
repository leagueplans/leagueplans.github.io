package ddm.ui.dom.player.item.inventory

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import ddm.ui.dom.common.{Modal, ToastHub}
import ddm.ui.model.player.item.Stack

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object InventoryHeader {
  def apply(
    stacks: Signal[List[(Stack, List[Int])]],
    modalController: Modal.Controller,
    toastPublisher: ToastHub.Publisher
  ): L.Element =
    L.div(
      L.cls(Styles.header),
      L.span(
        L.cls(Styles.title, DepositoryStyles.header, PanelStyles.header),
        L.img(
          L.cls(Styles.inventoryIcon, DepositoryStyles.icon),
          L.src(icon),
          L.alt("Inventory icon")
        ),
        "Inventory",
      ),
      ExportBankTagsButton(
        stacks,
        modalController, 
        toastPublisher
      ).amend(L.cls(Styles.exportButton))
    )

  @js.native @JSImport("/images/inventory-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/item/inventory/inventoryHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val title: String = js.native
    val inventoryIcon: String = js.native
    val exportButton: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }
}
