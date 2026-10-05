package com.leagueplans.ui.dom.planning.player.item.inventory.panel

import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.model.player.item.{BankTags, Depository, ItemStack}
import com.leagueplans.uicommon.dom.{Tooltip, ToastHub}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.{Failure, Success}

/** Beneath the inventory, as the bank has its footer: how many slots are taken, and a button that
  * copies the inventory as a RuneLite bank tag tab.
  */
object InventoryFooter {
  /** The name of the bank tag tab the inventory is copied as */
  private val tagName = "leagueplans"

  def apply(stacks: Signal[List[ItemStack]], toasts: ToastHub.Publisher, tooltip: Tooltip): L.Div =
    L.div(
      L.cls(Styles.footer),
      L.span(L.cls(Styles.label), "Slots"),
      L.span(L.cls(Styles.used), L.text <-- stacks.map(stacks => s"${stacks.size}/${Depository.Kind.Inventory.capacity}")),
      L.span(L.cls(Styles.spacer)),
      Card.withTooltip(
        L.button(
          L.cls(Styles.tags),
          L.tpe("button"),
          L.aria.label("Copy a RuneLite bank tag for the inventory"),
          FontAwesome.icon(FreeSolid.faTag),
          L.disabled <-- stacks.map(_.isEmpty),
          L.onClick.compose(_.sample(stacks)) --> (copyTags(_, toasts))
        ),
        stacks.map(stacks =>
          if (stacks.isEmpty) "The inventory is empty, so there's nothing to tag"
          else "Copy a RuneLite bank tag for the inventory"
        ),
        tooltip
      )
    )

  private def copyTags(stacks: List[ItemStack], toasts: ToastHub.Publisher): Unit = {
    import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global
    window.navigator.clipboard.writeText(BankTags.layout(tagName, stacks)).toFuture.onComplete {
      case Success(_) =>
        toasts.publish(
          ToastHub.Type.Success,
          8.seconds,
          "Copied the inventory as a bank tag tab",
          Some(s"In the game's bank, right-click the + tab and choose Import tag tab. It needs RuneLite's Bank Tags plugin.")
        )
      case Failure(_) =>
        toasts.publish(
          ToastHub.Type.Error,
          6.seconds,
          "Couldn't copy to the clipboard",
          Some("The browser didn't allow it. Try again after clicking on the page.")
        )
    }
  }

  @js.native @JSImport("/styles/planning/player/item/inventory/panel/inventoryFooter.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val footer: String = js.native
    val label: String = js.native
    val used: String = js.native
    val spacer: String = js.native
    val tags: String = js.native
  }
}
