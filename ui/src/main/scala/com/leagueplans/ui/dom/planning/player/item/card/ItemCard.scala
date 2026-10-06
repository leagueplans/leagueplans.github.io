package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.ItemActionRunner
import com.leagueplans.ui.dom.planning.player.item.card.ItemCardParts.*
import com.leagueplans.ui.model.plan.{ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.model.player.item.ItemActions.{Action, CardButton, Holding}
import com.leagueplans.ui.model.player.item.{ItemActions, ItemStack}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}

/** The card that opens on a held stack, showing the buttons `ItemActions.cardButtons` gives it.
  * One Amount box drives every button that takes an amount, and those buttons repeat it in their
  * labels, such as "Bank 25".
  *
  * Counts and limits come from the state that new effects are applied to, which can differ from
  * the state on show.
  */
object ItemCard {
  /** @param initialAmount what the Amount box starts on, such as the bank's quantity setting */
  def apply(
    holding: Holding,
    initialAmount: String,
    runner: ItemActionRunner,
    requirementObserver: Signal[Option[Observer[Requirement]]],
    cache: Cache,
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val amountText = Var(initialAmount)
    val amount = amountText.signal.map(parseAmount)

    def run(run: Action => Unit, action: Action): Unit = {
      run(action)
      close()
    }

    def wholeButton(button: CardButton.Whole): L.Span =
      Card.withTooltip(
        L.button(
          L.cls(Card.Styles.button),
          L.tpe("button"),
          button.label,
          L.disabled <-- runner.canRun.map(!_),
          L.onClick.compose(_.sample(runner.run, runner.playerAtInsertion).collect { case (Some(runAction), player) => (runAction, player) }) -->
            ((runAction, player) => button.act(player).foreach(run(runAction, _)))
        ),
        noFocusTip(runner.run),
        tooltip
      )

    def amountButton(button: CardButton.WithAmount): L.Span =
      Card.withTooltip(
        L.button(
          L.cls(if (button.secondary) Card.Styles.ghost else Card.Styles.button),
          L.tpe("button"),
          L.children <-- amount.map {
            case Some(ItemQuantity.Max) => List(L.textToTextNode(button.maxLabel.getOrElse(s"${button.label} all")))
            case Some(ItemQuantity.Exact(n)) => List(L.textToTextNode(s"${button.label} "), L.span(L.cls(Card.Styles.amount), n.withCommas))
            case None => List(L.textToTextNode(s"${button.label} "), L.span(L.cls(Card.Styles.amount), "?"))
          },
          L.disabled <-- Signal.combine(runner.canRun, amount).map((canRun, amount) =>
            !canRun || amount.forall(button.unavailable(_).nonEmpty)
          ),
          L.onClick.compose(
            _.sample(runner.run, amount, runner.playerAtInsertion).collect { case (Some(runAction), Some(n), player) => (runAction, n, player) }
          ) --> ((runAction, n, player) => button.act(n, player).foreach(run(runAction, _)))
        ),
        Signal.combine(noFocusTip(runner.run), amount).map((noFocus, amount) =>
          if (noFocus.nonEmpty) noFocus else amount.flatMap(button.unavailable).getOrElse("")
        ),
        tooltip
      )

    val buttons = ItemActions.cardButtons(holding, cache.items)
    val wholeButtons = buttons.collect { case button: CardButton.Whole => wholeButton(button) }
    val amountButtons = buttons.collect { case button: CardButton.WithAmount => amountButton(button) }

    L.div(
      L.cls(Card.Styles.card),
      header(holding.item, holding.noted, ItemStack(holding.item, holding.noted, 1), close),
      facts(holding.item, runner.playerAtInsertion),
      noFocusNotice(runner.run),
      L.when(wholeButtons.nonEmpty)(L.div(L.cls(Card.Styles.row), wholeButtons)),
      L.when(amountButtons.nonEmpty)(
        // Laid out as the add card is, with the labels in a column of their own
        L.div(
          L.cls(Card.Styles.well, Card.Styles.form),
          amountLabel(inputID),
          amountControls(inputID, amountText),
          // Across the whole card, so they start beneath the labels
          L.span(L.cls(Card.Styles.controls, Card.Styles.wide), amountButtons),
          L.child.maybe <-- amountText.signal.map(amountProblem(_).map(warning))
        )
      ),
      footer(holding.item, leading = requireButton(holding.item, requirementObserver, undoToasts, tooltip, close))
    )
  }

  private val inputID = "item-card-amount"
}
