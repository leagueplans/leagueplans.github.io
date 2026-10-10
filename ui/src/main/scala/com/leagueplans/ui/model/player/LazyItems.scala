package com.leagueplans.ui.model.player

import com.leagueplans.common.model.Item
import io.circe.scalajs.decodeJs

import scala.collection.immutable.AbstractMap
import scala.collection.mutable
import scala.scalajs.js

object LazyItems {
  /** @param json the items as parsed from items.json, each with an `id` */
  def apply(json: js.Array[js.Dynamic]): LazyItems =
    new LazyItems(json.iterator.map(item => Item.ID(item.id.asInstanceOf[Int]) -> item).toMap)
}

/** The game's items, decoded as they're first looked up.
  *
  * There are tens of thousands of items, and decoding them all takes long enough to hold up
  * showing a plan, while a plan only refers to a few hundred. Anything that goes through every
  * item, such as the bank's search, decodes the rest. [[decodeInBackground]] does that ahead of
  * time, a few at a time.
  *
  * Items are checked when the data is scraped, so one failing to decode is a bug. It's thrown
  * from wherever the item was looked up.
  */
final class LazyItems private (json: Map[Item.ID, js.Dynamic]) extends AbstractMap[Item.ID, Item] {
  private val decoded = mutable.Map.empty[Item.ID, Item]

  def get(id: Item.ID): Option[Item] =
    decoded.get(id).orElse(json.get(id).map { itemJson =>
      val item = decodeJs[Item](itemJson).fold(error => throw error, identity)
      decoded.update(id, item)
      item
    })

  def iterator: Iterator[(Item.ID, Item)] =
    json.keysIterator.map(id => id -> get(id).get)

  override def contains(id: Item.ID): Boolean = json.contains(id)
  override def keySet: Set[Item.ID] = json.keySet
  override def size: Int = json.size
  override def knownSize: Int = json.size

  // Nothing changes the items, so these only exist to satisfy Map
  def removed(id: Item.ID): Map[Item.ID, Item] = Map.from(iterator).removed(id)
  def updated[V >: Item](id: Item.ID, value: V): Map[Item.ID, V] = Map.from(iterator).updated(id, value)

  /** Decodes the items that haven't been yet, `batchSize` at a time between other work, so that
    * they're ready before anything needs them all
    *
    * @param onFailure told if an item can't be decoded, after which the rest are left
    */
  def decodeInBackground(batchSize: Int, onFailure: Throwable => Unit): Unit = {
    val remaining = json.keysIterator.filterNot(decoded.contains)

    def next(): Unit =
      try {
        var count = 0
        while (count < batchSize && remaining.hasNext) {
          get(remaining.next())
          count += 1
        }
        if (remaining.hasNext) js.timers.setTimeout(0)(next()): Unit
      } catch {
        case error: Throwable => onFailure(error)
      }

    next()
  }
}
