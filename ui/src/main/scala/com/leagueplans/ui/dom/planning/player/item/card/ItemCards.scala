package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.model.plan.{Effect, Requirement}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.item.ItemTransfer
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.{Popover, Tooltip}
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier}
import org.scalajs.dom.{Element, KeyboardEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Opens item cards from the stacks in the Items section.
  *
  * @param transferSettings the bank's quantity setting, which the bank's cards start on
  * @param boundary the element cards prefer to stay within, so that they don't cover the plan
  */
final class ItemCards(
  popover: Popover,
  playerAtInsertion: Signal[Player],
  effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
  requirementObserver: Signal[Option[Observer[Requirement]]],
  cache: Cache,
  transferSettings: StrictSignal[ItemTransfer.Settings],
  undoToasts: UndoToasts,
  tooltip: Tooltip,
  boundary: () => Option[Element]
) {
  /** Clicking the stack, or right-clicking it, opens its card. Clicking it again closes it. */
  def trigger(holding: Holding): L.Modifier[L.HtmlElement] =
    L.inContext(node =>
      List(
        L.tabIndex(0),
        L.role("button"),
        L.aria.label(s"${holding.item.name}: open its card"),
        L.cls(ItemCards.Styles.trigger),
        L.cls(ItemCards.Styles.selected) <-- popover.isAnchoredTo(node.ref),
        popover.closesWithAnchor,
        // Shift-clicks are left for quick moves
        L.onClick.filter(!_.shiftKey).compose(_.sample(popover.isAnchoredTo(node.ref))) --> (isOpen =>
          if (isOpen) popover.close() else open(holding, node.ref)
        ),
        L.onContextMenu.handled --> (_ => open(holding, node.ref)),
        L.onKeyDown.filter(isActivation).preventDefault --> (_ => open(holding, node.ref))
      )
    )

  /** Whether a card is open on the element */
  def isOpenOn(element: Element): Signal[Boolean] =
    popover.isAnchoredTo(element)

  private val addDraft = AddItemCard.Draft()

  /** For the bank search's results: clicking one opens a card that adds the item */
  def addTrigger(item: Item): L.Modifier[L.HtmlElement] =
    L.inContext(node =>
      List(
        L.cls(ItemCards.Styles.selected) <-- popover.isAnchoredTo(node.ref),
        popover.closesWithAnchor,
        L.onClick.compose(_.sample(popover.isAnchoredTo(node.ref))) --> (isOpen =>
          if (isOpen) popover.close()
          else popover.open(
            node.ref,
            AddItemCard(item, addDraft, playerAtInsertion, effectObserver, requirementObserver, undoToasts, tooltip, () => popover.close()),
            boundary(),
            below = true
          )
        )
      )
    )

  def open(holding: Holding, anchor: Element): Unit = {
    tooltip.close()
    popover.open(
      anchor,
      ItemCard(holding, initialAmount(holding), playerAtInsertion, effectObserver, requirementObserver, cache, undoToasts, tooltip, () => popover.close()),
      boundary()
    )
  }

  /** A bank stack's card starts on the bank's quantity setting. An inventory stack that's a single
    * item, as unnoted unstackable items are, starts on 1. Other cards start on the whole stack. */
  private def initialAmount(holding: Holding): String =
    holding.place match {
      case Kind.Bank =>
        transferSettings.now().quantity match {
          case ItemTransfer.Quantity.One => "1"
          case ItemTransfer.Quantity.Five => "5"
          case ItemTransfer.Quantity.Ten => "10"
          case ItemTransfer.Quantity.X(amount) => amount.toString
          case ItemTransfer.Quantity.All => "Max"
        }
      case Kind.Inventory if !holding.noted && !holding.item.stackable => "1"
      case _ => "Max"
    }

  private def isActivation(event: KeyboardEvent): Boolean =
    event.key == "Enter" || event.key == " "
}

object ItemCards {
  @js.native @JSImport("/styles/planning/player/item/card/itemCards.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val trigger: String = js.native
    val selected: String = js.native
  }
}
