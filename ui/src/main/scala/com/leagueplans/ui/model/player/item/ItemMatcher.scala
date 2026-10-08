package com.leagueplans.ui.model.player.item

import com.leagueplans.common.model.Item
import com.leagueplans.uicommon.facades.fusejs.FuseOptions
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.scalajs.js

/** Finds items to add by name, forgiving typos, so "lobstr" finds lobsters.
  *
  * Matches are ranked by how closely they match and then by how near the start of the name the
  * match is, so "logs" puts Logs before Oak logs. A match anywhere in the name still counts, so
  * "poison" finds poisoned arrows.
  *
  * The name and the variants are searched as separate fields, with the name counting for more, so
  * "dragon" puts Dragon bones before items with a "Dragon" variant. The full name is searched too,
  * so that "bronze arrow poison" finds Bronze arrow with its "Poison" variant.
  *
  * Building the index is the expensive part, so build one matcher and reuse it.
  */
final class ItemMatcher(items: Iterable[Item]) {
  // Sorted, so that equally good matches come out in a stable order
  private val ordered = items.toVector.sorted
  private val fuse =
    Fuse(
      ordered.map(item => ItemMatcher.Entry(item.name, item.infobox.version, item.fullName)).toList,
      ItemMatcher.options
    )

  def rank(query: String): List[Item] = {
    val q = query.trim
    if (q.isEmpty) List.empty else fuse.searchIndices(q).map(ordered)
  }
}

object ItemMatcher {
  private final case class Entry(name: String, variants: List[String], fullName: String)

  private object Entry {
    given Codec[Entry] = deriveCodec
  }

  private def key(field: String, fieldWeight: Double): FuseOptions.KeyObject =
    new FuseOptions.KeyObject {
      val name: String = field
      weight = js.defined(fieldWeight)
    }

  private val options: FuseOptions =
    new FuseOptions {
      // Fuse favours a perfect match on any one field over close matches on the others. At two to
      // one, Archibald's "Dragon" variant came before every Dragon item, so the variants count
      // for far less
      keys = js.defined(js.Array(key("name", 1), key("fullName", 0.2), key("variants", 0.05)))
      // Strict enough that searches like "logs" match dozens of items rather than thousands
      threshold = js.defined(0.2)
      // Matches later in the name cost a little, rather than being ruled out
      distance = js.defined(1000)
    }
}
