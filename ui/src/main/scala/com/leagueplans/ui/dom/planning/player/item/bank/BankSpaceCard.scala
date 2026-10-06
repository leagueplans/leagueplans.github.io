package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.model.player.item.{BankSpace, Depository}
import com.leagueplans.ui.model.player.{Player, ViewerAccount}
import com.leagueplans.uicommon.dom.Popover
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}
import org.scalajs.dom.Element

/** Says where the bank's slots come from, and lets the viewer say what's true of their account,
  * which applies to every plan they open.
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
    boundary: () => Option[Element]
  ): L.Modifier[L.HtmlElement] =
    L.inContext(node =>
      List(
        L.aria.hasPopup(true),
        L.aria.expanded <-- popover.isAnchoredTo(node.ref),
        popover.closesWithAnchor,
        L.onClick.compose(_.sample(popover.isAnchoredTo(node.ref))) --> (isOpen =>
          if (isOpen) popover.close()
          else popover.open(node.ref, apply(player, account, () => popover.close()), boundary())
        )
      )
    )

  def apply(player: Signal[Player], account: Var[ViewerAccount], close: () => Unit): L.Div =
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
        L.span(L.cls(Card.Styles.label), "Your account"),
        check("Jagex Account", account.signal.map(_.jagexAccount), checked => account.update(_.copy(jagexAccount = checked))),
        check("Authenticator", account.signal.map(_.authenticator), checked => account.update(_.copy(authenticator = checked))),
        L.p(L.cls(Card.Styles.note), "These apply to every plan you open in this browser, not just this one.")
      )
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
