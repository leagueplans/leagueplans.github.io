package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.ItemActionRunner
import com.leagueplans.ui.dom.planning.player.item.card.ItemCardParts.{noFocusNotice, noFocusTip}
import com.leagueplans.ui.model.player.item.ItemActions.Action
import com.leagueplans.ui.model.player.item.{BankSpace, Depository, ItemActions}
import com.leagueplans.ui.model.player.{Player, ViewerAccount}
import com.leagueplans.uicommon.dom.{Popover, Tooltip}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, seqToModifier, textToTextNode}
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Says where the bank's slots come from. It sets a bank PIN or buys more space on the focused
  * step, and lets the viewer say what's true of their account, which applies to every plan they
  * open.
  *
  * The totals are the player's on show, while the actions are worked out from the state new
  * effects apply to.
  */
object BankSpaceCard {
  /** Clicking the element opens the card beside it, or closes it again
    *
    * @param boundary the element the card prefers to stay within
    */
  def trigger(
    popover: Popover,
    player: Signal[Player],
    account: Var[ViewerAccount],
    runner: ItemActionRunner,
    tooltip: Tooltip,
    boundary: () => Option[Element]
  ): L.Modifier[L.HtmlElement] =
    L.inContext(node =>
      List(
        L.aria.hasPopup(true),
        L.aria.expanded <-- popover.isAnchoredTo(node.ref),
        popover.closesWithAnchor,
        L.onClick.compose(_.sample(popover.isAnchoredTo(node.ref))) --> (isOpen =>
          if (isOpen) popover.close()
          else popover.open(node.ref, apply(player, account, runner, tooltip, () => popover.close()), boundary())
        )
      )
    )

  def apply(
    player: Signal[Player],
    account: Var[ViewerAccount],
    runner: ItemActionRunner,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val pin = runner.playerAtInsertion.map(ItemActions.setBankPin)
    val purchase = runner.playerAtInsertion.map(ItemActions.buyBankSpace)

    L.div(
      L.cls(Card.Styles.card),
      L.div(
        L.cls(Card.Styles.head),
        L.div(
          L.cls(Card.Styles.titles),
          L.div(L.cls(Card.Styles.name), "Bank space"),
          L.p(
            L.cls(Card.Styles.note),
            L.text <-- player.map(player =>
              s"${player.capacity(Depository.Kind.Bank).withCommas} slots: ${player.bankSpace.breakdown}"
            )
          )
        ),
        Card.closeButton(close)
      ),
      L.div(
        L.cls(Card.Styles.well),
        L.span(L.cls(Card.Styles.label), "This plan"),
        noFocusNotice(runner.run),
        // Each action with what it does beneath it
        action(
          actionButton(pin, "Set a bank PIN", "A bank PIN is already set", runner, tooltip, close),
          pin.map {
            case Some(_) => s"Setting a PIN unlocks ${BankSpace.unlockSlots} more slots."
            case None => s"A PIN is set, which unlocked ${BankSpace.unlockSlots} more slots."
          }
        ),
        action(
          actionButton(purchase, "Buy more space", s"All ${BankSpace.blockPrices.size} blocks are bought", runner, tooltip, close),
          Signal.combine(runner.playerAtInsertion, purchase).map((player, purchase) =>
            val bought = s"${player.bankSpace.blocksBought} of ${BankSpace.blockPrices.size} blocks bought"
            purchase.flatMap(_.detail).fold(s"$bought.")(detail => s"${detail.capitalize}. $bought.")
          )
        )
      ),
      L.div(
        L.cls(Card.Styles.well),
        L.span(L.cls(Card.Styles.label), "Your account"),
        check("Jagex Account", account.signal.map(_.jagexAccount), checked => account.update(_.copy(jagexAccount = checked))),
        check("Authenticator", account.signal.map(_.authenticator), checked => account.update(_.copy(authenticator = checked))),
        L.p(L.cls(Card.Styles.note), "These apply to every plan you open in this browser, not just this one.")
      )
    )
  }

  private def action(button: L.Span, explanation: Signal[String]): L.Div =
    L.div(
      L.cls(Styles.action),
      button,
      L.p(L.cls(Card.Styles.note, Styles.explanation), L.text <-- explanation)
    )

  @js.native @JSImport("/styles/planning/player/item/bank/bankSpaceCard.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val action: String = js.native
    val explanation: String = js.native
  }

  /** A button that runs an action on the focused step, labelled with its preview
    *
    * @param fallback the button's label while there's no action
    * @param unavailable why there's no action
    */
  private def actionButton(
    action: Signal[Option[Action]],
    fallback: String,
    unavailable: String,
    runner: ItemActionRunner,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Span =
    Card.withTooltip(
      L.button(
        L.cls(Card.Styles.button),
        L.tpe("button"),
        L.text <-- action.map(_.fold(fallback)(_.preview)),
        L.disabled <-- Signal.combine(runner.run, action).map((run, action) => run.isEmpty || action.isEmpty),
        L.onClick.compose(_.sample(runner.run, action).collect { case (Some(run), Some(action)) => (run, action) }) -->
          { (run, action) =>
            run(action)
            close()
          }
      ),
      Signal.combine(noFocusTip(runner.run), action).map((noFocus, action) =>
        if (noFocus.nonEmpty) noFocus else if (action.isEmpty) unavailable else ""
      ),
      tooltip
    )

  private def check(label: String, checked: Signal[Boolean], onChange: Boolean => Unit): L.Label =
    L.label(
      L.cls(Card.Styles.check),
      L.input(
        L.tpe("checkbox"),
        L.controlled(L.checked <-- checked, L.onClick.mapToChecked --> onChange)
      ),
      s"$label (+${BankSpace.unlockSlots} slots)"
    )
}
