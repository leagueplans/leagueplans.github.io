package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.player.item.{StackElement, StackIcon}
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.model.player.item.{Depository, ItemIdentity, ItemMatcher, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.dom.Tooltip
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Beneath the bank's own matches, the bank search lists every item that matches, including
  * ones already held, so that it doubles as the way to add items. The results are small tiles
  * that show enough to tell variants apart, with more in their tooltips.
  */
object AddResults {
  private val limit = 30

  def apply(
    query: Signal[String],
    playerSignal: Signal[Player],
    cache: Cache,
    itemCards: ItemCards,
    tooltip: Tooltip
  ): L.Div = {
    val matcher = ItemMatcher(cache.items.values)
    val matches = query.map(matcher.rank).distinct

    L.div(
      L.cls(Styles.results),
      L.p(
        L.cls(Styles.summary),
        L.text <-- matches.map(matches =>
          matches.size match {
            case 0 => "No items match."
            case 1 => "Add to the inventory: 1 item matches."
            case n if n <= limit => s"Add to the inventory: $n items match."
            case n => s"Add to the inventory: $n items match, showing the first $limit."
          }
        )
      ),
      L.ol(
        L.cls(Styles.tiles),
        L.children <-- matches.map(_.take(limit)).split(_.id)((_, item, _) =>
          L.li(tile(item, playerSignal, itemCards, tooltip))
        )
      )
    )
  }

  /** A tile with the item's icon and name, and its variant beneath. The examine text and held
    * counts are in its tooltip, to keep the tiles small. */
  private def tile(item: Item, playerSignal: Signal[Player], itemCards: ItemCards, tooltip: Tooltip): L.Button = {
    val identity = ItemIdentity(item)
    L.button(
      L.cls(Styles.tile),
      L.tpe("button"),
      L.aria.label(s"Add ${item.name}"),
      L.div(L.cls(Styles.icon), StackIcon(ItemStack(item, noted = false, quantity = 1))),
      L.div(
        L.cls(Styles.text),
        L.span(L.cls(Styles.name), identity.base),
        L.when(identity.variants.nonEmpty)(L.span(L.cls(Styles.variant), identity.variants.mkString(" · ")))
      ),
      itemCards.addTrigger(item),
      L.inContext(node =>
        tooltip.register(
          // As the stacks' tooltips are
          StackElement.tooltipContents(item, playerSignal.map(held(item, _)).map(held => Option.when(held.nonEmpty)(held))),
          FloatingConfig.basicTooltip(Placement.bottom, offset = 6),
          suppressed = itemCards.isOpenOn(node.ref)
        )
      )
    )
  }

  private def held(item: Item, player: Player): String = {
    def count(kind: Depository.Kind): Int =
      player.get(kind).contents.collect { case ((id, _), n) if id == item.id => n }.sum

    List(
      Option.when(count(Depository.Kind.Inventory) > 0)(s"${count(Depository.Kind.Inventory).withCommas} in inventory"),
      Option.when(count(Depository.Kind.Bank) > 0)(s"${count(Depository.Kind.Bank).withCommas} in bank")
    ).flatten.mkString(" · ")
  }

  @js.native @JSImport("/styles/planning/player/item/bank/addResults.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val results: String = js.native
    val summary: String = js.native
    val tiles: String = js.native
    val tile: String = js.native
    val icon: String = js.native
    val text: String = js.native
    val name: String = js.native
    val variant: String = js.native
  }
}
