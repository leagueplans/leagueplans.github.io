package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{AcceptedItems, InfoboxKey, Item}

object IDMap {
  def from(accepted: AcceptedItems): IDMap =
    IDMap(accepted.ids, accepted.nextID)
}

/** Which item each wiki infobox maps to, and which ID to hand out next. Saved as part of
  * [[AcceptedItems]], whose docs say why [[nextID]] only ever rises.
  */
final case class IDMap(mappings: Map[InfoboxKey, Item.ID], nextID: Int) {
  def get(key: InfoboxKey): Option[Item.ID] = mappings.get(key)

  def ids: Set[Item.ID] = mappings.values.toSet
}
