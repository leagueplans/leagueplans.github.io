package com.leagueplans.scrapereview.items.model

import com.leagueplans.common.model.{InfoboxKey, Item}

object IDAllocator {
  def from(idMap: IDMap): IDAllocator = IDAllocator(idMap, idMap.nextID)
}

/** Hands out item IDs, never reissuing one that has been used before.
  *
  * Immutable so that a reviewer changing their mind about one item cannot leave the
  * allocator holding state from a decision that no longer stands.
  */
final case class IDAllocator private (private val idMap: IDMap, nextID: Int) {

  /** The ID this key already has, if it has one. */
  def existing(key: InfoboxKey): Option[Item.ID] = idMap.get(key)

  /** Claims a fresh ID, returning it alongside an allocator that will not offer it
    * again.
    */
  def allocate: (Item.ID, IDAllocator) =
    (Item.ID(nextID), copy(nextID = nextID + 1))

  /** Gives each of `keys` an ID, in order, so a rerun over the same review produces the
    * same assignment.
    *
    * A key that already has an ID keeps it. In a first Apply no added key has one, but an
    * Apply that is repeated — or retried after failing partway — meets keys the earlier
    * attempt already numbered. Handing those out again would renumber every item the
    * scrape added.
    */
  def allocateAll(keys: Seq[InfoboxKey]): (Map[InfoboxKey, Item.ID], IDAllocator) =
    keys.foldLeft((Map.empty[InfoboxKey, Item.ID], this)) { case ((assigned, allocator), key) =>
      existing(key) match {
        case Some(id) =>
          (assigned + (key -> id), allocator)
        case None =>
          val (id, next) = allocator.allocate
          (assigned + (key -> id), next)
      }
    }
}
