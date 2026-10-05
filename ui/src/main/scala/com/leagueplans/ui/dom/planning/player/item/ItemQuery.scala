package com.leagueplans.ui.dom.planning.player.item

import com.leagueplans.common.model.Item

/** What's typed into the bank search. Held items match on any part of their full name, so
  * "poison" finds poisoned arrows. */
object ItemQuery {
  def isEmpty(query: String): Boolean =
    query.trim.isEmpty

  def matches(item: Item, query: String): Boolean =
    item.name.toLowerCase.contains(query.trim.toLowerCase)
}
