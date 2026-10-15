package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository

/** What a stop condition is checked against after a repetition.
  *
  * @param shortfalls how many more of each item the repetition tried to take from a place than the
  *                   place held, keyed by the item, the place and whether it's noted. Items are
  *                   never held below zero, so this is how a repetition shows that it would have
  *                   taken a count below zero.
  */
final case class LoopContext(
  player: Player,
  items: Item.ID => Item,
  shortfalls: Map[(Item.ID, Depository.Kind, Boolean), Int]
)
