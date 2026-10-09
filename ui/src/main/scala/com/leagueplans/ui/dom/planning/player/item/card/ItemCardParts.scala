package com.leagueplans.ui.dom.planning.player.item.card

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.plan.history.UndoToasts
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.{AmountStepKeys, StackIcon}
import com.leagueplans.ui.model.plan.{ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository.Kind
import com.leagueplans.ui.model.player.item.Depository.Kind.EquipmentSlot
import com.leagueplans.ui.model.player.item.{ItemIdentity, ItemStack, ItemWikiPage}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, seqToModifier, textToTextNode}

/** The parts the item card and the add card share: the head and foot, the counts held, the Amount
  * box, and the Make required button */
object ItemCardParts {
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
        L.inputMode("decimal"),
        L.controlled(L.value <-- amountText.signal, L.onInput.mapToValue --> amountText.writer),
        AmountStepKeys(amountText.writer)
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
    ItemQuantity.parse(text).toOption

  /** Why the actions can't take an amount typed into a card, if they can't */
  def amountProblem(text: String): Option[String] =
    Option.when(text.trim.nonEmpty)(ItemQuantity.parse(text)).flatMap {
      case Right(_) => None
      case Left(ItemQuantity.Problem.AboveMax) => Some(s"A stack can hold at most ${Int.MaxValue.withCommas}.")
      case Left(_) => Some("Type an amount from 1, such as 250 or 1.5k, or pick one.")
    }

  def warning(text: String): L.HtmlElement =
    L.p(L.cls(Card.Styles.warning), text)

  /** Makes the focused step need the item at its start, in the inventory or equipped */
  def requireButton(
    item: Item,
    requirementObserver: Signal[Option[Observer[Requirement]]],
    undoToasts: UndoToasts,
    tooltip: Tooltip,
    close: () => Unit
  ): L.Span = {
    val requirement = Requirement.held(item)
    Card.withTooltip(
      L.button(
        L.cls(Card.Styles.ghost),
        L.tpe("button"),
        "Make required",
        L.disabled <-- requirementObserver.map(_.isEmpty),
        L.onClick.compose(_.sample(requirementObserver).collectSome) --> { observer =>
          observer.onNext(requirement)
          close()
          undoToasts.report(s"Required ${item.fullName}", Some("A requirement of the focused step"), duration = UndoToasts.brief)
        }
      ),
      noFocusTip(requirementObserver, s"The focused step will check that this item is ${requirement.where.description}"),
      tooltip
    )
  }

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
    Card.footer(ItemWikiPage.url(item), leading)

  /** How many of the item are held in each place, noted or not */
  def facts(item: Item, playerSignal: Signal[Player]): L.Div =
    L.div(
      L.cls(Card.Styles.facts),
      L.children <-- playerSignal.map(player => factList(item, player))
    )

  private def factList(item: Item, player: Player): List[L.Span] = {
    def count(kind: Kind): Int =
      player.get(kind).contents.collect { case ((id, _), n) if id == item.id => n }.sum

    val equipped = EquipmentSlot.values.map(count).sum
    List(
      L.span("Inventory ", L.b(count(Kind.Inventory).withCommas)),
      L.span("Bank ", L.b(count(Kind.Bank).withCommas))
    ) ++ Option.when(item.equipmentType.nonEmpty)(
      // Only one of an unstackable item can be equipped, so whether it is says it all
      L.span("Equipped ", L.b(if (item.stackable) equipped.withCommas else if (equipped > 0) "yes" else "no"))
    )
  }
}
