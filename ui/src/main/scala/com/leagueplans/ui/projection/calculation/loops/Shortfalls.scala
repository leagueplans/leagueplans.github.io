package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.player.item.Depository

/** How many more of each item something tried to take from a place than the place held, keyed by
  * the item, the place, and whether it's noted */
type Shortfalls = Map[(Item.ID, Depository.Kind, Boolean), Int]

object Shortfalls {
  val none: Shortfalls = Map.empty

  def combine(a: Shortfalls, b: Shortfalls): Shortfalls =
    b.foldLeft(a) { case (acc, (key, n)) => acc.updated(key, acc.getOrElse(key, 0) + n) }
}
