package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.ItemActionRunner
import com.leagueplans.ui.model.plan.{ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.{Depository, ItemActions, ItemEffects, ItemStack}
import com.leagueplans.uicommon.dom.Tooltip
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}

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
    runner: ItemActionRunner,
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val amount = draft.amount.signal.map(ItemCardParts.parseAmount)
    // Max only works where each item takes a slot of its own
    val unavailable =
      Signal.combine(amount, draft.target.signal, draft.noted.signal).map {
        case (Some(ItemQuantity.Max), target, noted) if !ItemEffects.canFill(item, noted && item.noteable, target) =>
          Some(ItemActions.fillReason)
        case _ =>
          None
      }

    L.div(
      L.cls(Card.Styles.card),
      ItemCardParts.header(item, noted = false, ItemStack(item, noted = false, quantity = 1), close),
      ItemCardParts.facts(item, runner.playerAtInsertion),
      ItemCardParts.noFocusNotice(runner.run),
      // Labels in one column and controls in the other, so the controls line up
      L.div(
        L.cls(Card.Styles.well, Card.Styles.form),
        ItemCardParts.amountLabel(amountID),
        ItemCardParts.amountControls(amountID, draft.amount),
        L.span(L.cls(Card.Styles.label), "Into"),
        L.span(
          L.cls(Card.Styles.controls),
          L.span(
            L.cls(Card.Styles.segments),
            L.role("group"),
            L.aria.label("Where to add the item"),
            List(Depository.Kind.Inventory -> "Inventory", Depository.Kind.Bank -> "Bank").map((target, label) =>
              L.button(
                L.cls(Card.Styles.segment),
                L.tpe("button"),
                label,
                L.aria.pressed <-- draft.target.signal.map(_ == target).map(_.toString),
                L.onClick.mapTo(target) --> draft.target.writer
              )
            )
          ),
          L.when(item.noteable)(
            L.label(
              L.cls(Card.Styles.check),
              L.input(
                L.tpe("checkbox"),
                L.controlled(L.checked <-- draft.noted.signal, L.onClick.mapToChecked --> draft.noted.writer)
              ),
              "Noted"
            )
          )
        ),
        // Across the whole card, as on the item card
        L.span(
          L.cls(Card.Styles.controls, Card.Styles.wide),
          Card.withTooltip(
            L.button(
              L.cls(Card.Styles.button),
              L.tpe("button"),
              L.text <-- amount.map {
                case Some(ItemQuantity.Max) => "Add until full"
                case n => s"Add ${n.map(ItemActions.describe(item, _)).getOrElse(item.name)}"
              },
              L.disabled <-- Signal.combine(runner.canRun, amount, unavailable).map((canRun, amount, unavailable) =>
                !canRun || amount.isEmpty || unavailable.nonEmpty
              ),
              L.onClick.compose(
                _.sample(runner.run, amount, draft.target.signal, draft.noted.signal).collect {
                  case (Some(run), Some(n), target, noted) => (run, n, target, noted)
                }
              ) --> { (run, n, target, noted) =>
                run(ItemActions.add(item, n, target, noted && target == Depository.Kind.Inventory))
                close()
              }
            ),
            Signal.combine(ItemCardParts.noFocusTip(runner.run), unavailable).map((noFocus, unavailable) =>
              if (noFocus.nonEmpty) noFocus else unavailable.getOrElse("")
            ),
            tooltip
          )
        ),
        L.child.maybe <-- draft.amount.signal.map(ItemCardParts.amountProblem(_).map(ItemCardParts.warning))
      ),
      ItemCardParts.footer(item, leading = ItemCardParts.requireButton(item, requirementObserver, undoToasts, tooltip, close))
    )
  }

  private val amountID = "add-item-card-amount"
}
