package ddm.ui.model.player.item

import ddm.common.model.Item

object ItemCache {
  def apply(items: Set[Item]): ItemCache =
    ItemCache(items.map(item => item.id -> item).toMap)
}

final case class ItemCache(raw: Map[Item.ID, Item]) {
  def apply(id: Item.ID): Item =
    raw(id)

  def itemise(depository: Depository): List[(Stack, List[Int])] =
    depository
      .contents
      .toList
      .map { case ((id, noted), count) => (this(id), noted, count) }
      .sortBy { case (item, noted, _) => (item.name, noted) }
      .map {
        case (item, noted, count) if item.stackable || noted || depository.kind.autoStack =>
          Stack(item, noted) -> List(count)
        case (item, noted, count) =>
          Stack(item, noted) -> List.fill(count)(1)
      }
}
