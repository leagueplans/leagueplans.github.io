package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.model.plan.{Effect, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.{Action, Holding}
import com.leagueplans.ui.model.player.item.{ItemActions, ItemIdentity, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier, textToTextNode}

import scala.util.Try

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
    requirementObserver: Signal[Option[Observer[Requirement]]],
    cache: Cache,
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Div = {
    val held = playerAtInsertion.map(ItemActions.held(holding, _)).distinct
    val amountText = Var("")
    val amount = amountText.signal.map(parseAmount)
    val isHeldPlace = holding.place == Kind.Inventory || holding.place == Kind.Bank
    val overHeld = Signal.combine(amount, held).map((amount, held) => isHeldPlace && amount.exists(_ > held))

    def run(observer: Observer[Effect | Seq[Effect]], action: Action): Unit = {
      observer.onNext(action.effects)
      close()
      undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
    }

    def amountButton(label: String, ghost: Boolean, limitedByHeld: Boolean, tip: String = "")(toAction: Int => Action): L.Span =
      Card.withTooltip(
        L.button(
          L.cls(if (ghost) Card.Styles.ghost else Card.Styles.button),
          L.tpe("button"),
          label,
          " ",
          L.span(L.cls(Card.Styles.amount), L.text <-- amount.map(_.map(format).getOrElse("?"))),
          L.disabled <-- Signal.combine(effectObserver, amount, overHeld).map((observer, amount, over) =>
            observer.isEmpty || amount.isEmpty || (limitedByHeld && over)
          ),
          L.onClick.compose(_.sample(effectObserver, amount).collect { case (Some(observer), Some(n)) => (observer, n) }) -->
            ((observer, n) => run(observer, toAction(n)))
        ),
        noFocusTip(effectObserver, tip),
        tooltip
      )

    def wholeButton(label: String)(toAction: Player => Option[Action]): L.Span =
      Card.withTooltip(
        L.button(
          L.cls(Card.Styles.button),
          L.tpe("button"),
          label,
          L.disabled <-- effectObserver.map(_.isEmpty),
          L.onClick.compose(_.sample(effectObserver, playerAtInsertion).collect { case (Some(observer), player) => (observer, player) }) -->
            ((observer, player) => toAction(player).foreach(run(observer, _)))
        ),
        noFocusTip(effectObserver),
        tooltip
      )

    val wholeActions: List[L.Span] =
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

    // The buttons that can't take more than the stack holds, by label
    val limitedActions: List[(String, Int => Action)] =
      holding.place match {
        case Kind.Inventory =>
          Option.when(ItemActions.canBank(holding))("Bank" -> ((n: Int) => ItemActions.bank(holding, ItemQuantity.Exact(n)))).toList :+
            ("Remove" -> (ItemActions.remove(holding, _)))
        case Kind.Bank =>
          List(
            Some("Withdraw" -> ((n: Int) => ItemActions.withdraw(holding, ItemQuantity.Exact(n), noted = false))),
            Option.when(ItemActions.canWithdrawNoted(holding))(
              "Withdraw noted" -> ((n: Int) => ItemActions.withdraw(holding, ItemQuantity.Exact(n), noted = true))
            ),
            Some("Remove" -> (ItemActions.remove(holding, _)))
          ).flatten
        case _: EquipmentSlot =>
          List.empty
      }

    val amountActions: List[L.Span] =
      limitedActions.map((label, toAction) =>
        amountButton(label, ghost = label == "Remove", limitedByHeld = true)(toAction)
      ) :+ amountButton(
        "Add",
        ghost = true,
        limitedByHeld = false,
        tip = s"Adds new copies to the ${if (holding.place == Kind.Bank) "bank" else "inventory"}"
      )(ItemActions.addMore(holding, _))

    val limitedLabels = limitedActions.map(_._1)

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
      L.div(L.cls(Card.Styles.row), wholeActions :+ requireButton(holding.item, requirementObserver, undoToasts, tooltip, close)),
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
        L.child.maybe <-- Signal.combine(amountText.signal, overHeld, held).map((text, over, held) =>
          amountProblem(text)
            .orElse(Option.when(over)(overHeldWarning(held, limitedLabels)))
            .map(warning)
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

  /** The tooltip for a button that's disabled while no step is focused: why, or else `otherwise` */
  def noFocusTip(observer: Signal[Option[?]], otherwise: String = ""): Signal[String] =
    observer.map(observer => if (observer.isEmpty) noFocusReason else otherwise)

  /** An amount typed into a card, if it's one the actions can take */
  def parseAmount(text: String): Option[Int] =
    text.trim.toIntOption.filter(_ > 0)

  /** Why the actions can't take an amount typed into a card, if they can't */
  def amountProblem(text: String): Option[String] =
    text.trim match {
      case "" => None
      case trimmed =>
        Try(BigInt(trimmed)).toOption match {
          case Some(n) if n > Int.MaxValue =>
            Some(s"The most an action can move is ${format(Int.MaxValue)}, the most a stack can hold.")
          case Some(n) if n > 0 =>
            None
          case _ =>
            Some("Amounts are whole numbers, from 1.")
        }
    }

  def warning(text: String): L.HtmlElement =
    L.p(L.cls(Card.Styles.warning), text)

  /** Such as "There are only 5 here, so Bank and Remove can move at most 5." */
  private def overHeldWarning(held: Int, labels: List[String]): String = {
    val actions = labels match {
      case Nil => "the actions"
      case init :+ last if init.nonEmpty => s"${init.mkString(", ")} and $last"
      case only => only.mkString
    }
    s"There ${if (held == 1) "is" else "are"} only ${format(held)} here, so $actions can move at most ${format(held)}."
  }

  /** Makes the focused step need the item at its start, in the inventory or worn */
  def requireButton(
    item: Item,
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Span = {
    val where = if (item.equipmentType.nonEmpty) "in the inventory or worn" else "in the inventory"
    Card.withTooltip(
      L.button(
        L.cls(Card.Styles.ghost),
        L.tpe("button"),
        "Make required",
        L.disabled <-- requirementObserver.map(_.isEmpty),
        L.onClick.compose(_.sample(requirementObserver).collectSome) --> { observer =>
          observer.onNext(Requirement.held(item))
          close()
          undoToasts.report(s"Required ${item.name}", Some("A requirement of the focused step"), duration = UndoToasts.brief)
        }
      ),
      noFocusTip(requirementObserver, s"The focused step will need this $where when it starts"),
      tooltip
    )
  }

  private val inputID = "item-card-amount"

  /** The icon, name, variants and examine text shared by the item cards */
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
