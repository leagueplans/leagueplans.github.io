package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.model.plan.{Effect, ItemQuantity}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.{Action, Holding}
import com.leagueplans.ui.model.player.item.{ItemActions, ItemIdentity, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}

/** The card that opens on a held stack. One Amount box drives every button that takes an amount,
  * and those buttons repeat it in their labels, such as "Bank 25".
  *
  * Counts and limits come from the state that new effects are applied to, which can differ from
  * the state on show.
  */
object ItemCard {
  def apply(
    holding: Holding,
    playerAtInsertion: Signal[Player],
    effectObserver: Signal[Option[Observer[Effect | Seq[Effect]]]],
    cache: Cache,
    undoToasts: UndoToasts,
    close: () => Unit
  ): L.Div = {
    val held = playerAtInsertion.map(ItemActions.held(holding, _)).distinct
    val amountText = Var("")
    val amount = amountText.signal.map(_.trim.toIntOption.filter(_ > 0))
    val isHeldPlace = holding.place == Kind.Inventory || holding.place == Kind.Bank
    val overHeld = Signal.combine(amount, held).map((amount, held) => isHeldPlace && amount.exists(_ > held))

    def run(observer: Observer[Effect | Seq[Effect]], action: Action): Unit = {
      observer.onNext(action.effects)
      close()
      undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
    }

    def amountButton(label: String, ghost: Boolean, limitedByHeld: Boolean)(toAction: Int => Action): L.Button =
      L.button(
        L.cls(if (ghost) Card.Styles.ghost else Card.Styles.button),
        L.tpe("button"),
        label,
        " ",
        L.span(L.cls(Card.Styles.amount), L.text <-- amount.map(_.map(format).getOrElse("?"))),
        L.disabled <-- Signal.combine(effectObserver, amount, overHeld).map((observer, amount, over) =>
          observer.isEmpty || amount.isEmpty || (limitedByHeld && over)
        ),
        noFocusTitle(effectObserver),
        L.onClick.compose(_.sample(effectObserver, amount).collect { case (Some(observer), Some(n)) => (observer, n) }) -->
          ((observer, n) => run(observer, toAction(n)))
      )

    def wholeButton(label: String)(toAction: Player => Option[Action]): L.Button =
      L.button(
        L.cls(Card.Styles.button),
        L.tpe("button"),
        label,
        L.disabled <-- effectObserver.map(_.isEmpty),
        noFocusTitle(effectObserver),
        L.onClick.compose(_.sample(effectObserver, playerAtInsertion).collect { case (Some(observer), player) => (observer, player) }) -->
          ((observer, player) => toAction(player).foreach(run(observer, _)))
      )

    val wholeActions: List[L.Button] =
      holding.place match {
        case Kind.Inventory if ItemActions.canWear(holding) =>
          List(wholeButton(ItemActions.wearLabel(holding.item))(ItemActions.wear(holding, _, cache.items)))
        case _: EquipmentSlot =>
          List(
            Some(wholeButton("Unequip")(_ => ItemActions.unequip(holding, Kind.Inventory))),
            Option.when(holding.item.bankable != Item.Bankable.No)(
              wholeButton("Bank")(_ => ItemActions.unequip(holding, Kind.Bank))
            )
          ).flatten
        case _ =>
          List.empty
      }

    val amountActions: List[L.Button] =
      (holding.place match {
        case Kind.Inventory =>
          List(
            Option.when(ItemActions.canBank(holding))(
              amountButton("Bank", ghost = false, limitedByHeld = true)(n => ItemActions.bank(holding, ItemQuantity.Exact(n)))
            ),
            Some(amountButton("Remove", ghost = true, limitedByHeld = true)(ItemActions.remove(holding, _)))
          )
        case Kind.Bank =>
          List(
            Some(amountButton("Withdraw", ghost = false, limitedByHeld = true)(n => ItemActions.withdraw(holding, ItemQuantity.Exact(n), noted = false))),
            Option.when(ItemActions.canWithdrawNoted(holding))(
              amountButton("Withdraw noted", ghost = false, limitedByHeld = true)(n => ItemActions.withdraw(holding, ItemQuantity.Exact(n), noted = true))
            ),
            Some(amountButton("Remove", ghost = true, limitedByHeld = true)(ItemActions.remove(holding, _)))
          )
        case _: EquipmentSlot =>
          List.empty
      }).flatten :+ amountButton("Add", ghost = true, limitedByHeld = false)(ItemActions.addMore(holding, _)).amend(
        noFocusTitle(effectObserver, s"Adds new copies to the ${if (holding.place == Kind.Bank) "bank" else "inventory"}")
      )

    L.div(
      L.cls(Card.Styles.card),
      // Fills the Amount box with the whole stack, once its size is known
      held --> (held => if (amountText.now().isEmpty) amountText.set(math.max(held, 1).toString)),
      header(holding.item, holding.noted, ItemStack(holding.item, holding.noted, 1), close),
      L.div(
        L.cls(Card.Styles.facts),
        L.children <-- playerAtInsertion.map(player => facts(holding.item, player))
      ),
      noFocusNotice(effectObserver),
      L.when(wholeActions.nonEmpty)(L.div(L.cls(Card.Styles.row), wholeActions)),
      L.div(
        L.cls(Card.Styles.well),
        L.div(
          L.cls(Card.Styles.row),
          L.label(L.cls(Card.Styles.label), L.forId(inputID), "Amount"),
          L.input(
            L.cls(Card.Styles.number),
            L.idAttr(inputID),
            L.tpe("number"),
            L.minAttr("1"),
            L.stepAttr("1"),
            L.controlled(L.value <-- amountText.signal, L.onInput.mapToValue --> amountText.writer)
          ),
          L.child <-- held.map(quickAmounts(isHeldPlace, _, amountText.writer))
        ),
        L.div(L.cls(Card.Styles.row), amountActions),
        L.child.maybe <-- Signal.combine(overHeld, held).map((over, held) =>
          Option.when(over)(
            L.p(L.cls(Card.Styles.warning), s"Only ${format(held)} held here, so only Add can use this amount.")
          )
        )
      )
    )
  }

  private val noFocusReason = "No step is focused. Choose a step in the plan to add these changes to it."

  /** Explains, above a card's actions, why they're disabled while no step is focused */
  def noFocusNotice(observer: Signal[Option[?]]): L.Modifier[L.HtmlElement] =
    L.child.maybe <-- observer.map(observer =>
      Option.when(observer.isEmpty)(
        L.p(L.cls(Card.Styles.notice), FontAwesome.icon(FreeSolid.faCircleInfo), noFocusReason)
      )
    )

  /** Gives a button that's disabled while no step is focused the reason as its title */
  def noFocusTitle(observer: Signal[Option[?]], otherwise: String = ""): L.Modifier[L.HtmlElement] =
    L.title <-- observer.map(observer => if (observer.isEmpty) noFocusReason else otherwise)

  private val inputID = "item-card-amount"

  /** The icon, name, variants and identifying details shared by the item cards */
  def header(item: Item, noted: Boolean, iconStack: ItemStack, close: () => Unit): L.Div = {
    val identity = ItemIdentity(item)
    L.div(
      L.cls(Card.Styles.head),
      L.div(L.cls(Card.Styles.icon), StackIcon(iconStack)),
      L.div(
        L.cls(Card.Styles.titles),
        L.div(
          L.cls(Card.Styles.name),
          identity.base,
          identity.variants.map(variant => L.span(L.cls(Card.Styles.chip), variant)),
          L.when(noted)(L.span(L.cls(Card.Styles.chip), "noted"))
        ),
        L.when(item.examine.nonEmpty)(L.p(L.cls(Card.Styles.note), item.examine)),
        L.p(L.cls(Card.Styles.note), details(item))
      ),
      Card.closeButton(close)
    )
  }

  /** Properties and game ID, such as "stackable · noteable · ID 882" */
  def details(item: Item): String =
    (ItemIdentity.properties(item) ++ item.gameID.map(id => s"ID $id")).mkString(" · ")

  private def facts(item: Item, player: Player): List[L.Span] = {
    def count(kind: Kind): Int =
      player.get(kind).contents.collect { case ((id, _), n) if id == item.id => n }.sum

    val worn = EquipmentSlot.values.exists(slot => count(slot) > 0)
    List(
      L.span("Inventory ", L.b(format(count(Kind.Inventory)))),
      L.span("Bank ", L.b(format(count(Kind.Bank))))
    ) ++ Option.when(item.equipmentType.nonEmpty)(L.span("Worn ", L.b(if (worn) "yes" else "no")))
  }

  private def quickAmounts(isHeldPlace: Boolean, held: Int, amountText: Observer[String]): L.Node = {
    val values =
      List(1, 5, 10).filter(n => !isHeldPlace || n < held).map(n => n.toString -> n) ++
        Option.when(isHeldPlace && held > 1)("All" -> held)

    if (values.isEmpty)
      L.emptyNode
    else
      L.span(
        L.cls(Card.Styles.segments),
        values.map((label, n) =>
          L.button(
            L.cls(Card.Styles.segment),
            L.tpe("button"),
            label,
            L.onClick --> (_ => amountText.onNext(n.toString))
          )
        )
      )
  }

  private def format(n: Int): String =
    n.withCommas
}
