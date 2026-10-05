package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.model.plan.{Effect, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.{Depository, ItemActions, ItemStack}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, textToTextNode}

/** The card for adding an item found by the bank search. Items go to the inventory unless
  * another place is chosen.
  */
object AddItemCard {
  /** What the add card was last set to. It doesn't depend on the focused step, so it's kept for
    * the next item added, even after the focus changes. */
  final class Draft {
    val amount: Var[String] = Var("1")
    val target: Var[Depository.Kind] = Var(Depository.Kind.Inventory)
    val noted: Var[Boolean] = Var(false)
  }

  def apply(
    item: Item,
    draft: Draft,
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val amount = draft.amount.signal.map(ItemCard.parseAmount)

    L.div(
      L.cls(Card.Styles.card),
      ItemCard.header(item, noted = false, ItemStack(item, noted = false, quantity = 1), close),
      ItemCard.noFocusNotice(effectObserver),
      L.div(
        L.cls(Card.Styles.well),
        L.div(
          L.cls(Card.Styles.row),
          L.label(L.cls(Card.Styles.label), L.forId(amountID), "Amount"),
          L.input(
            L.cls(Card.Styles.number),
            L.idAttr(amountID),
            L.tpe("number"),
            L.minAttr("1"),
            L.stepAttr("1"),
            L.controlled(L.value <-- draft.amount.signal, L.onInput.mapToValue --> draft.amount.writer)
          )
        ),
        L.div(
          L.cls(Card.Styles.row),
          L.label(L.cls(Card.Styles.label), L.forId(targetID), "Into"),
          L.select(
            L.cls(Card.Styles.select),
            L.idAttr(targetID),
            L.option(L.value("inventory"), "Inventory"),
            L.option(L.value("bank"), "Bank"),
            L.controlled(
              L.value <-- draft.target.signal.map(target => if (target == Depository.Kind.Bank) "bank" else "inventory"),
              L.onChange.mapToValue.map(value =>
                if (value == "bank") Depository.Kind.Bank else Depository.Kind.Inventory
              ) --> draft.target.writer
            )
          ),
          L.when(item.noteable)(
            L.label(
              L.cls(Card.Styles.label),
              L.input(
                L.tpe("checkbox"),
                L.controlled(L.checked <-- draft.noted.signal, L.onClick.mapToChecked --> draft.noted.writer)
              ),
              " Noted"
            )
          )
        ),
        L.div(
          L.cls(Card.Styles.row),
          Card.withTooltip(
            L.button(
              L.cls(Card.Styles.button),
              L.tpe("button"),
              L.text <-- amount.map(n => s"Add ${n.map(n => ItemActions.describe(item, ItemQuantity.Exact(n))).getOrElse(item.name)}"),
              L.disabled <-- Signal.combine(effectObserver, amount).map((observer, amount) => observer.isEmpty || amount.isEmpty),
              L.onClick.compose(
                _.sample(effectObserver, amount, draft.target.signal, draft.noted.signal).collect {
                  case (Some(observer), Some(n), target, noted) => (observer, n, target, noted)
                }
              ) --> { (observer, n, target, noted) =>
                val action = ItemActions.add(item, n, target, noted && target == Depository.Kind.Inventory)
                observer.onNext(action.effects)
                close()
                undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
              }
            ),
            ItemCard.noFocusTip(effectObserver),
            tooltip
          )
        ),
        L.child.maybe <-- draft.amount.signal.map(ItemCard.amountProblem(_).map(ItemCard.warning))
      ),
      L.div(L.cls(Card.Styles.row), ItemCard.requireButton(item, requirementObserver, undoToasts, tooltip, close))
    )
  }

  private val amountID = "add-item-card-amount"
  private val targetID = "add-item-card-target"
}
