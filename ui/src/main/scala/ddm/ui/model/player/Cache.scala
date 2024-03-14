package ddm.ui.model.player

import ddm.common.model.{Item, LeagueTask}
import ddm.ui.model.player.diary.DiaryTask
import ddm.ui.model.player.item.{Depository, Stack}

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
  def itemise(depository: Depository): List[(Stack, List[Int])] =
    depository
      .contents
      .toList
      .map { case ((id, noted), count) => (items(id), noted, count) }
      .sortBy((item, noted, _) => (item.name, noted))
      .map {
        case (item, noted, count) if item.stackable || noted || depository.kind.autoStack =>
          Stack(item, noted) -> List(count)
        case (item, noted, count) =>
          Stack(item, noted) -> List.fill(count)(1)
      }
}
