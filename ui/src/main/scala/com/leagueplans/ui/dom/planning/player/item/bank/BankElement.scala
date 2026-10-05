package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.dom.planning.player.item.drag.ItemDrag
import com.leagueplans.ui.dom.planning.player.item.{DepositoryStacks, ItemQuery, StackElement}
import com.leagueplans.ui.model.player.item.ItemActions.Holding
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.ui.model.player.item.{Depository, ItemStack, ItemTransfer}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, eventPropToProcessor, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object BankElement {
  /** @param playerAtInsertion the state new effects apply to, for the add results' held counts
    * @param query the bank search, which also dims the inventory's stacks that don't match, and
    *              finds items to add
    */
  def apply(
    playerSignal: Signal[Player],
    playerAtInsertion: Signal[Player],
    query: Var[String],
    cache: Cache,
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip,
    footer: Signal[Int] => L.Div
  ): L.Div = {
    val bankSignal = playerSignal.map(_.get(Depository.Kind.Bank))
    val stacks = bankSignal.map(cache.itemise)
    val matchingStacks =
      Signal.combine(stacks, query.signal).map((stacks, query) =>
        if (ItemQuery.isEmpty(query)) stacks else stacks.filter(stack => ItemQuery.matches(stack.item, query))
      )

    L.div(
      L.cls(DepositoryStyles.depository, PanelStyles.panel),
      itemDrag.target(ItemTransfer.Target.Bank),
      L.headerTag(
        L.cls(DepositoryStyles.header, PanelStyles.header),
        L.img(L.cls(Styles.icon, DepositoryStyles.icon), L.src(icon), L.alt("Bank icon")),
        "Bank"
      ),
      searchBar(query, playerSignal, cache),
      L.div(
        L.cls(Styles.scroller),
        DepositoryStacks(
          matchingStacks,
          columnCount = 8,
          rowCount = 100,
          overflowRowCount = 10,
          toStackElement(itemCards, itemDrag, tooltip),
          tooltip,
          fillWidth = true
        ),
        L.child.maybe <-- Signal.combine(stacks, matchingStacks, query.signal).map((all, matching, query) =>
          Option.when(matching.isEmpty)(
            L.p(
              L.cls(Styles.emptyState),
              if (all.nonEmpty && !ItemQuery.isEmpty(query)) "Nothing in the bank matches." else "The bank is empty."
            )
          )
        ),
        // The same search finds items to add
        L.child.maybe <-- query.signal.map(ItemQuery.isEmpty).distinct.map(isEmpty =>
          Option.when(!isEmpty)(AddResults(query.signal, playerAtInsertion, cache, itemCards))
        )
      ),
      footer(stacks.map(_.size))
    )
  }

  @js.native @JSImport("/images/bank-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/player/item/bank/bankElement.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val search: String = js.native
    val searchInput: String = js.native
    val searchCounts: String = js.native
    val scroller: String = js.native
    val emptyState: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/item/depositoryElement.module.css", JSImport.Default)
  private object DepositoryStyles extends js.Object {
    val depository: String = js.native
    val header: String = js.native
    val icon: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def searchBar(query: Var[String], playerSignal: Signal[Player], cache: Cache): L.Div =
    L.div(
      L.cls(Styles.search),
      L.input(
        L.cls(Styles.searchInput),
        L.tpe("search"),
        L.placeholder("Search the bank, or find any item to add"),
        L.aria.label("Search the bank, or find any item to add"),
        L.autoComplete("off"),
        L.controlled(L.value <-- query.signal, L.onInput.mapToValue --> query.writer)
      ),
      L.child.maybe <-- Signal.combine(playerSignal, query.signal).map((player, query) =>
        Option.when(!ItemQuery.isEmpty(query))(
          L.span(L.cls(Styles.searchCounts), counts(player, query, cache))
        )
      )
    )

  private def counts(player: Player, query: String, cache: Cache): String = {
    def matching(kind: Depository.Kind): Int =
      cache.itemise(player.get(kind)).count(stack => ItemQuery.matches(stack.item, query))

    val inventory = matching(Depository.Kind.Inventory)
    val bank = s"${matching(Depository.Kind.Bank)} in bank"
    if (inventory == 0) bank else s"$bank · $inventory in inventory"
  }

  private def toStackElement(
    itemCards: ItemCards,
    itemDrag: ItemDrag,
    tooltip: Tooltip
  )(stack: ItemStack): L.Div =
    StackElement(
      stack,
      tooltip,
      // Beside the stack, since the bank's top often sits at the top of the page, where a tooltip
      // above the panel would be cut off. Stack tooltips let the pointer through, so they never
      // stand in the way of the stacks they cover.
      tooltipConfig = FloatingConfig.basicTooltip(Placement.bottom, offset = 6),
      hideTooltip = itemCards.isOpenOn
    ).amend(
      itemCards.trigger(Holding(stack.item, stack.noted, Depository.Kind.Bank)),
      itemDrag.source(Holding(stack.item, stack.noted, Depository.Kind.Bank))
    )
}
