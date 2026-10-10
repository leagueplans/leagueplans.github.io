package com.leagueplans.ui.model.player

import com.leagueplans.common.model.{GridTile, Item, LeagueTask}
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.item.{Depository, ItemStack}
import io.circe.Decoder
import io.circe.scalajs.decodeJs
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable
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
    
  /** Reads the game data. The files are fetched rather than bundled with the code, so that the page
    * and the projection worker share one download through the browser's cache. */
  def load(): js.Promise[Either[(String, Throwable), Cache]] =
    // The imports are read inside the dynamic import, which keeps them out of the main module
    js.dynamicImport(
      DataUrls(DataUrls.items, DataUrls.quests, DataUrls.diaryTasks, DataUrls.leagueTasks, DataUrls.gridTiles)
    ).toFuture.flatMap(load).toJSPromise

  private def load(urls: DataUrls): Future[Either[(String, Throwable), Cache]] = {
    // Started together, so that they download in parallel
    val items = fetchJson("items", urls.items)
    val quests = fetchJson("quests", urls.quests)
    val diaryTasks = fetchJson("diary tasks", urls.diaryTasks)
    val leagueTasks = fetchJson("league tasks", urls.leagueTasks)
    val gridTiles = fetchJson("grid tiles", urls.gridTiles)

    (for {
      itemsJson <- items
      questsJson <- quests
      diaryTasksJson <- diaryTasks
      leagueTasksJson <- leagueTasks
      gridTilesJson <- gridTiles
    } yield for {
      itemsJson <- itemsJson
      quests <- questsJson.flatMap(decode[Set[Quest]]("quests", _))
      diaryTasks <- diaryTasksJson.flatMap(decode[Set[DiaryTask]]("diary tasks", _))
      leagueTasks <- leagueTasksJson.flatMap(decode[Set[LeagueTask]]("league tasks", _))
      gridTiles <- gridTilesJson.flatMap(decode[Set[GridTile]]("grid tiles", _))
    } yield Cache(
      LazyItems(itemsJson.asInstanceOf[js.Array[js.Dynamic]]),
      quests.map(quest => quest.id -> quest).toMap,
      diaryTasks.map(task => task.id -> task).toMap,
      leagueTasks.map(task => task.id -> task).toMap,
      gridTiles.map(tile => tile.id -> tile).toMap
    ))
  }

  private def fetchJson(key: String, url: String): Future[Either[(String, Throwable), js.Any]] =
    dom.fetch(url).toFuture
      .flatMap(response =>
        if (response.ok) response.json().toFuture.map(Right(_))
        else Future.successful(Left((key, RuntimeException(s"HTTP ${response.status} for $url"))))
      )
      .recover { case error => Left((key, error)) }

  private def decode[T : Decoder](key: String, json: js.Any): Either[(String, Throwable), T] =
    decodeJs[T](json).left.map((key, _))

  /** Where the game data is served from. Imported dynamically, since the imports can only be
    * resolved by Vite, not in tests. */
  private final case class DataUrls(
    items: String,
    quests: String,
    diaryTasks: String,
    leagueTasks: String,
    gridTiles: String
  )

  private object DataUrls {
    @js.native @JSImport("/data/items.json?url", JSImport.Default)
    def items: String = js.native

    @js.native @JSImport("/data/quests.json?url", JSImport.Default)
    def quests: String = js.native

    @js.native @JSImport("/data/diaryTasks.json?url", JSImport.Default)
    def diaryTasks: String = js.native

    @js.native @JSImport("/data/leagueTasks.json?url", JSImport.Default)
    def leagueTasks: String = js.native

    @js.native @JSImport("/data/gridMaster.json?url", JSImport.Default)
    def gridTiles: String = js.native
  }
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
      .sortBy((item, noted, _) => (item.fullName, noted))
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
