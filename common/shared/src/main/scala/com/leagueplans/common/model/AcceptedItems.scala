package com.leagueplans.common.model

import io.circe.{Decoder, Encoder}

object AcceptedItems {
  // Items are written as a list of triples rather than an object, since an InfoboxKey is a
  // structure and JSON object keys can only be strings.
  given Decoder[AcceptedItems] =
    Decoder.forProduct2[AcceptedItems, Int, Vector[(InfoboxKey, Item.ID, ItemData)]]("nextID", "items")(
      AcceptedItems.apply
    )

  given Encoder[AcceptedItems] =
    Encoder.forProduct2[AcceptedItems, Int, Vector[(InfoboxKey, Item.ID, ItemData)]]("nextID", "items")(accepted =>
      // Sorted so that a scrape which changes nothing produces no diff
      (accepted.nextID, accepted.items.sortBy(_._1))
    )
}

/** The item data that review has accepted, which is what the app's items are built from and
  * what the next scrape is compared against: each wiki infobox, the ID we gave its item, and
  * what was scraped from it.
  *
  * [[nextID]] is the ID to hand out next, and only ever rises. Reusing the ID of a retired
  * item would silently repoint every plan that still referred to it at whatever unrelated
  * item next claimed the number, which is worse than leaving those plans pointing at
  * something that no longer exists. Retired IDs leave no other trace, so it can't be worked
  * out from the items.
  */
final case class AcceptedItems(nextID: Int, items: Vector[(InfoboxKey, Item.ID, ItemData)]) {
  def data: Vector[(InfoboxKey, ItemData)] =
    items.map((key, _, data) => key -> data)

  def ids: Map[InfoboxKey, Item.ID] =
    items.map((key, id, _) => key -> id).toMap
}
