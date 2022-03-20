package ddm.ui.model.player.item

import ddm.common.model.Item

object ItemCache {
  def apply(items: Set[Item]): ItemCache =
    ItemCache(items.map(item => item.id -> item).toMap)
}

final case class ItemCache(raw: Map[Item.ID, Item]) {
  def apply(id: Item.ID): Item =
    raw(id)

  def itemise(depository: Depository): List[(Item, Int)] =
    depository
      .contents
      .toList
      .map { case (id, count) => (this(id), count) }
      .flatMap {
        case (item, count) if item.stackable || depository.stackAll =>
          List((item, count))
        case (item, count) =>
          List.fill(count)((item, 1))
      }
      .sortBy { case (item, _) => item.name }
}
