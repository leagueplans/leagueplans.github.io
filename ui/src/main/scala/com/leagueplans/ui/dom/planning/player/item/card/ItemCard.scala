package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.model.plan.{Effect, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.ItemActions.{Action, Holding}
import com.leagueplans.ui.model.player.item.{ItemActions, ItemEffects, ItemIdentity, ItemStack, ItemWikiPage}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
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
    // A whole stack is Max, so its effects take whatever is held where they apply
    val amountText = Var("Max")
    val amount = amountText.signal.map(parseAmount)

    def run(observer: Observer[Effect | Seq[Effect]], action: Action): Unit = {
      observer.onNext(action.effects)
      close()
      undoToasts.report(action.report, action.detail, duration = UndoToasts.brief)
    }

    /** @param allLabel the button's label when the amount is Max, if not "<label> all"
      * @param unavailable why the button can't take an amount, if there's an amount it can't take
      */
    def amountButton(
      label: String,
      ghost: Boolean,
      allLabel: Option[String] = None,
      unavailable: ItemQuantity => Option[String] = _ => None
    )(toAction: ItemQuantity => Action): L.Span =
      Card.withTooltip(
        L.button(
          L.cls(if (ghost) Card.Styles.ghost else Card.Styles.button),
          L.tpe("button"),
          L.children <-- amount.map {
            case Some(ItemQuantity.Max) => List(L.textToTextNode(allLabel.getOrElse(s"$label all")))
            case Some(ItemQuantity.Exact(n)) => List(L.textToTextNode(s"$label "), L.span(L.cls(Card.Styles.amount), format(n)))
            case None => List(L.textToTextNode(s"$label "), L.span(L.cls(Card.Styles.amount), "?"))
          },
          L.disabled <-- Signal.combine(effectObserver, amount).map((observer, amount) =>
            observer.isEmpty || amount.forall(unavailable(_).nonEmpty)
          ),
          L.onClick.compose(_.sample(effectObserver, amount).collect { case (Some(observer), Some(n)) => (observer, n) }) -->
            ((observer, n) => run(observer, toAction(n)))
        ),
        Signal.combine(noFocusTip(effectObserver), amount).map((noFocus, amount) =>
          if (noFocus.nonEmpty) noFocus else amount.flatMap(unavailable).getOrElse("")
        ),
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

    // Amounts above what's held are allowed: the plan shows the problem, and it can help while
    // other steps are still being changed
    val placeActions: List[L.Span] =
      holding.place match {
        case Kind.Inventory =>
          Option.when(ItemActions.canBank(holding))(
            amountButton("Bank", ghost = false)(ItemActions.bank(holding, _))
          ).toList :+ amountButton("Remove", ghost = true)(ItemActions.remove(holding, _))
        case Kind.Bank =>
          List(
            Some(amountButton("Withdraw", ghost = false)(ItemActions.withdraw(holding, _, noted = false))),
            Option.when(ItemActions.canWithdrawNoted(holding))(
              amountButton("Withdraw noted", ghost = false, allLabel = Some("Withdraw all noted"))(
                ItemActions.withdraw(holding, _, noted = true)
              )
            ),
            Some(amountButton("Remove", ghost = true)(ItemActions.remove(holding, _)))
          ).flatten
        case _: EquipmentSlot =>
          List.empty
      }

    val amountActions: List[L.Span] =
      placeActions :+ amountButton(
        "Add",
        ghost = true,
        allLabel = Some("Add until full"),
        unavailable = {
          case ItemQuantity.Max if !ItemEffects.canFill(holding.item, holding.noted, Kind.Inventory) => Some(fillReason)
          case _ => None
        }
      )(ItemActions.addMore(holding, _))

    L.div(
      L.cls(Card.Styles.card),
      header(holding.item, holding.noted, ItemStack(holding.item, holding.noted, 1), close),
      L.div(
        L.cls(Card.Styles.facts),
        L.children <-- playerAtInsertion.map(player => facts(holding.item, player))
      ),
      noFocusNotice(effectObserver),
      L.when(wholeActions.nonEmpty)(L.div(L.cls(Card.Styles.row), wholeActions)),
      L.div(
        L.cls(Card.Styles.well),
        amountRow(inputID, amountText),
        L.div(L.cls(Card.Styles.row), amountActions),
        L.child.maybe <-- amountText.signal.map(amountProblem(_).map(warning))
      ),
      footer(holding.item, leading = requireButton(holding.item, requirementObserver, undoToasts, tooltip, close))
    )
  }

  /** Why Add can't take Max: only items that each take a slot can be added until the inventory's
    * full */
  val fillReason: String = "Adding until full only works for items that take an inventory slot each"

  def amountRow(id: String, amountText: Var[String]): L.Div =
    L.div(L.cls(Card.Styles.row), amountLabel(id), amountControls(id, amountText))

  def amountLabel(id: String): L.Label =
    L.label(L.cls(Card.Styles.label), L.forId(id), "Amount")

  /** The Amount box, with the same quick amounts on every card: 1, 5, 10 and Max */
  def amountControls(id: String, amountText: Var[String]): L.Span =
    L.span(
      L.cls(Card.Styles.controls),
      L.input(
        L.cls(Card.Styles.number),
        L.idAttr(id),
        L.tpe("text"),
        L.inputMode("numeric"),
        L.controlled(L.value <-- amountText.signal, L.onInput.mapToValue --> amountText.writer)
      ),
      L.span(
        L.cls(Card.Styles.segments),
        List("1", "5", "10", "Max").map(value =>
          L.button(
            L.cls(Card.Styles.segment),
            L.tpe("button"),
            value,
            L.aria.pressed <-- amountText.signal.map(_.trim.equalsIgnoreCase(value).toString),
            L.onClick.mapTo(value) --> amountText.writer
          )
        )
      )
    )

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

  /** An amount typed into a card, if it's one the actions can take. Max is everything that's held,
    * or as much as fits. All is taken to mean the same. */
  def parseAmount(text: String): Option[ItemQuantity] =
    text.trim.replace(",", "") match {
      case word if word.equalsIgnoreCase("all") || word.equalsIgnoreCase("max") => Some(ItemQuantity.Max)
      case number => number.toIntOption.filter(_ > 0).map(ItemQuantity.Exact(_))
    }

  /** Why the actions can't take an amount typed into a card, if they can't */
  def amountProblem(text: String): Option[String] =
    text.trim.replace(",", "") match {
      case "" => None
      case trimmed if parseAmount(trimmed).nonEmpty => None
      case trimmed =>
        Try(BigInt(trimmed)).toOption match {
          case Some(n) if n > Int.MaxValue => Some(s"A stack can hold at most ${format(Int.MaxValue)}.")
          case _ => Some("Type a whole number from 1, or pick an amount.")
        }
    }

  def warning(text: String): L.HtmlElement =
    L.p(L.cls(Card.Styles.warning), text)

  /** Makes the focused step need the item at its start, in the inventory or worn */
  def requireButton(
    item: Item,
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Span = {
    val where = if (item.equipmentType.nonEmpty) "is in the inventory or worn" else "is in the inventory"
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
      noFocusTip(requirementObserver, s"The focused step will check that this item $where"),
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
        L.when(item.examine.nonEmpty)(L.p(L.cls(Card.Styles.note), item.examine))
      ),
      Card.closeButton(close)
    )
  }

  /** The foot of the item cards, with a link to the item's wiki page */
  /** @param leading anything to show on the left, opposite the wiki link */
  def footer(item: Item, leading: L.Modifier[L.HtmlElement] = L.emptyMod): L.Div =
    L.div(
      L.cls(Card.Styles.foot),
      leading,
      L.a(
        L.cls(Card.Styles.wikiLink),
        L.href(ItemWikiPage.url(item)),
        L.target("_blank"),
        L.rel("noopener noreferrer"),
        L.img(L.src(wikiIcon), L.alt("")),
        "Open on the wiki"
      )
    )

  @js.native @JSImport("/images/wiki-icon.png", JSImport.Default)
  private val wikiIcon: String = js.native

  private def facts(item: Item, player: Player): List[L.Span] = {
    def count(kind: Kind): Int =
      player.get(kind).contents.collect { case ((id, _), n) if id == item.id => n }.sum

    val worn = EquipmentSlot.values.exists(slot => count(slot) > 0)
    List(
      L.span("Inventory ", L.b(format(count(Kind.Inventory)))),
      L.span("Bank ", L.b(format(count(Kind.Bank))))
    ) ++ Option.when(item.equipmentType.nonEmpty)(L.span("Worn ", L.b(if (worn) "yes" else "no")))
  }

  private def format(n: Int): String =
    n.withCommas
}
