package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.uicommon.facades.fusejs.FuseOptions
import com.leagueplans.uicommon.wrappers.fusejs.Fuse

import scala.scalajs.js

/** Finds items to add by name, forgiving typos, so "lobstr" finds lobsters.
  *
  * Matches are ranked by how closely they match and then by how near the start of the name the
  * match is, so "logs" puts Logs before Oak logs. A match anywhere in the name still counts, so
  * "poison" finds poisoned arrows.
  *
  * Building the index is the expensive part, so build one matcher and reuse it.
  */
final class ItemMatcher(items: Iterable[Item]) {
  // Sorted, so that equally good matches come out in a stable order
  private val ordered = items.toVector.sorted
  private val fuse = Fuse(ordered.map(_.name).toList, ItemMatcher.options)

  def rank(query: String): List[Item] = {
    val q = query.trim
    if (q.isEmpty) List.empty else fuse.searchIndices(q).map(ordered)
  }
}

object ItemMatcher {
  private val options: FuseOptions =
    new FuseOptions {
      // Strict enough that searches like "logs" match dozens of items rather than thousands
      threshold = js.defined(0.2)
      // Matches later in the name cost a little, rather than being ruled out
      distance = js.defined(1000)
    }
}
