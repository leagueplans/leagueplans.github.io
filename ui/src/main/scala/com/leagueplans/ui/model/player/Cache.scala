package com.leagueplans.ui.model.player

import com.leagueplans.common.model.{GridTile, Item, LeagueTask}
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import io.circe.Decoder
import io.circe.scalajs.decodeJs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Cache {
  def apply(
    items: Set[Item],
    quests: Set[Quest],
    diaryTasks: Set[DiaryTask],
    leagueTasks: Set[LeagueTask],
    gridTiles: Set[GridTile]
  ): Cache =
    Cache(
      items.map(item => item.id -> item).toMap,
      quests.map(quest => quest.id -> quest).toMap,
      diaryTasks.map(task => task.id -> task).toMap,
      leagueTasks.map(task => task.id -> task).toMap,
      gridTiles.map(task => task.id -> task).toMap,
    )
    
  def load(): js.Promise[Either[(String, Throwable), Cache]] =
    js.dynamicImport(
      for {
        items <- decode[Set[Item]]("items", itemsJson)
        quests <- decode[Set[Quest]]("quests", questsJson)
        diaryTasks <- decode[Set[DiaryTask]]("diary tasks", diaryTasksJson)
        leagueTasks <- decode[Set[LeagueTask]]("league tasks", leagueTasksJson)
        gridTiles <- decode[Set[GridTile]]("grid tiles", gridTilesJson)
      } yield Cache(items, quests, diaryTasks, leagueTasks, gridTiles)
    )
  
  private def decode[T : Decoder](key: String, json: js.Object): Either[(String, Throwable), T] =
    decodeJs[T](json).left.map((key, _))

  @js.native @JSImport("/data/items.json", JSImport.Default)
  private def itemsJson: js.Object = js.native

  @js.native @JSImport("/data/quests.json", JSImport.Default)
  private def questsJson: js.Object = js.native

  @js.native @JSImport("/data/diaryTasks.json", JSImport.Default)
  private def diaryTasksJson: js.Object = js.native

  @js.native @JSImport("/data/leagueTasks.json", JSImport.Default)
  private def leagueTasksJson: js.Object = js.native

  @js.native @JSImport("/data/gridMaster.json", JSImport.Default)
  private def gridTilesJson: js.Object = js.native
}

final case class Cache(
  items: Map[Item.ID, Item],
  quests: Map[Int, Quest],
  diaryTasks: Map[Int, DiaryTask],
  leagueTasks: Map[Int, LeagueTask],
  gridTiles: Map[Int, GridTile]
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
    
  val gridTilesByColumn: Map[Int, Set[Int]] =
    gridTiles.values.groupBy(_.column).map((column, tiles) => column -> tiles.map(_.id).toSet)
    
  val gridTilesByRow: Map[Int, Set[Int]] =
    gridTiles.values.groupBy(_.row).map((row, tiles) => row -> tiles.map(_.id).toSet)
}
