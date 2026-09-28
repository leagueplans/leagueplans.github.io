package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item}
import io.circe.{Decoder, Encoder}

// The mappings are written as a list of pairs rather than an object, since an
// InfoboxKey is a structure and JSON object keys can only be strings.
object IDMap {
  given Decoder[IDMap] =
    Decoder.forProduct2[IDMap, Int, Vector[(InfoboxKey, Item.ID)]]("nextID", "mappings")(
      (nextID, mappings) => IDMap(mappings.toMap, nextID)
    )

  given Encoder[IDMap] =
    Encoder.forProduct2[IDMap, Int, Vector[(InfoboxKey, Item.ID)]]("nextID", "mappings")(idMap =>
      // Sorted so that a scrape which changes nothing produces no diff.
      (idMap.nextID, idMap.mappings.toVector.sortBy(_._1))
    )
}

/** Which item each wiki page maps to, and which ID to hand out next.
  *
  * [[nextID]] only ever rises. Reusing the ID of a retired item would silently repoint
  * every plan that still referred to it at whatever unrelated item next claimed the
  * number, which is worse than leaving those plans pointing at something that no longer
  * exists.
  */
final case class IDMap(mappings: Map[InfoboxKey, Item.ID], nextID: Int) {
  def get(key: InfoboxKey): Option[Item.ID] = mappings.get(key)

  def ids: Set[Item.ID] = mappings.values.toSet
}
