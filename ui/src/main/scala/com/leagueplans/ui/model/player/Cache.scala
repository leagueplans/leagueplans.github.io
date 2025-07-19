package com.leagueplans.ui.model.player

import com.leagueplans.common.model.{Item, LeagueTask}
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}

object Cache {
  def apply(items: Set[Item], quests: Set[Quest], diaryTasks: Set[DiaryTask], leagueTasks: Set[LeagueTask]): Cache =
    Cache(
      items.map(item => item.id -> item).toMap,
      quests.map(quest => quest.id -> quest).toMap,
      diaryTasks.map(task => task.id -> task).toMap,
      leagueTasks.map(task => task.id -> task).toMap
    )
}

final case class Cache(
  items: Map[Item.ID, Item],
  quests: Map[Int, Quest],
  diaryTasks: Map[Int, DiaryTask],
  leagueTasks: Map[Int, LeagueTask]
) {
  def itemise(depository: Depository): List[ItemStack] =
    depository
      .contents
      .toList
      .map { case ((id, noted), count) => (items(id), noted, count) }
      .sortBy((item, noted, _) => (item.name, noted))
      .flatMap {
        case (item, noted, quantity) if isStacked(item, noted, depository.kind) =>
          List(ItemStack(item, noted, quantity))
        case (item, noted, quantity) =>
          List.fill(quantity)(ItemStack(item, noted, quantity = 1))
      }
  
  private def isStacked(item: Item, noted: Boolean, depository: Depository.Kind): Boolean =
    item.stackable || noted || (
      depository == Depository.Kind.Bank && item.bankable == Item.Bankable.Yes(stacks = true)
    )
}
