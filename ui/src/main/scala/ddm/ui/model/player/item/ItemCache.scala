package ddm.ui.model.player.item

import ddm.common.model.Item

object ItemCache {
  def apply(items: Set[Item]): ItemCache =
    ItemCache(items.map(item => item.id -> item).toMap)
}

final case class ItemCache(raw: Map[Item.ID, Item]) {
  def apply(id: Item.ID): Item =
    raw(id)

  def itemise(depository: Depository): List[(Item, List[Int])] =
    depository
      .contents
      .toList
      .map { case (id, count) => this(id) -> count }
      .sortBy { case (item, _) => item.name }
      .map {
        case (item, count) if item.stackable || depository.kind.autoStack =>
          item -> List(count)
        case (item, count) =>
          item -> List.fill(count)(1)
      }
}
