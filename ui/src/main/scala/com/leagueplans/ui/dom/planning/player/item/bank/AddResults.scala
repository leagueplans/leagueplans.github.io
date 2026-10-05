package com.leagueplans.ui.dom.planning.player.item.bank

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.player.card.Card
import com.leagueplans.ui.dom.planning.player.item.StackIcon
import com.leagueplans.ui.dom.planning.player.item.card.ItemCards
import com.leagueplans.ui.model.player.item.{Depository, ItemIdentity, ItemMatcher, ItemStack}
import com.leagueplans.ui.model.player.{Cache, Player}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Beneath the bank's own matches, the bank search lists every item that matches, including
  * ones already held, so that it doubles as the way to add items. Results show enough to tell
  * variants apart: the variant as a chip, examine text and held counts.
  */
object AddResults {
  private val limit = 30

  def apply(query: Signal[String], playerSignal: Signal[Player], cache: Cache, itemCards: ItemCards): L.Div = {
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
        L.cls(Styles.rows),
        L.children <-- matches.map(_.take(limit)).split(_.id)((_, item, _) =>
          L.li(row(item, playerSignal, itemCards))
        )
      )
    )
  }

  private def row(item: Item, playerSignal: Signal[Player], itemCards: ItemCards): L.Button = {
    val identity = ItemIdentity(item)
    L.button(
      L.cls(Styles.row),
      L.tpe("button"),
      L.div(L.cls(Styles.icon), StackIcon(ItemStack(item, noted = false, quantity = 1))),
      L.div(
        L.cls(Styles.text),
        L.span(
          L.cls(Styles.name),
          identity.base,
          identity.variants.map(variant => L.span(L.cls(Card.Styles.chip), variant))
        ),
        L.span(L.cls(Styles.details), item.examine)
      ),
      L.span(L.cls(Styles.held), L.text <-- playerSignal.map(held(item, _))),
      L.span(L.cls(Styles.plus), "+"),
      itemCards.addTrigger(item)
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
    val rows: String = js.native
    val row: String = js.native
    val icon: String = js.native
    val text: String = js.native
    val name: String = js.native
    val details: String = js.native
    val held: String = js.native
    val plus: String = js.native
  }
}
