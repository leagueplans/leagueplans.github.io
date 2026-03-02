package com.leagueplans.ui.integration

import com.leagueplans.common.model.{GridTile, Item, LeagueTask}
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.model.player.{Cache, Quest}
import com.leagueplans.ui.testutils.readFile
import io.circe.Decoder
import io.circe.parser.decode
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class CacheIntegrationTest
  extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TryValues {

  private lazy val cache: Cache =
    Cache(
      readData[Set[Item]]("items"),
      readData[Set[Quest]]("quests"),
      readData[Set[DiaryTask]]("diaryTasks"),
      readData[Set[LeagueTask]]("leagueTasks"),
      readData[Set[GridTile]]("gridMaster")
    )

  private def readData[T : Decoder](fileName: String): T = {
    val filePath = toDataPath(fileName)
    withClue(s"Failed to read $filePath:")(
      readFile(filePath).flatMap(bytes =>
        decode[T](String(bytes)).toTry
      ).success.value
    )
  }

  private def toDataPath(fileName: String): String =
    s"ui/src/main/web/data/$fileName.json"

  "CacheIntegrationTest" - {
    "All IDs referenced in mode settings should be indexed in the cache" - Mode.all.foreach { mode =>
      mode.name - {
        "Depositories" - {
          mode.settings.initialPlayer.depositories.foreach { (kind, depository) =>
            val contents = depository.contents.keys
            if (contents.nonEmpty) {
              kind.name - {
                depository.contents.keys.foreach((id, noted) =>
                  id.toString in {
                    val item = cache.items.get(id).value
                    if (noted) withClue("Item is noted, but the cache suggests it cannot be noted")(
                      item.noteable shouldBe true
                    ): Unit
                  }
                )
              }
            }
          }
        }

        val completedQuests = mode.settings.initialPlayer.completedQuests
        if (completedQuests.nonEmpty) {
          "Quests" - {
            completedQuests.foreach(id =>
              id.toString in (
                cache.quests.get(id) shouldBe defined
              )
            )
          }
        }

        val completedDiaries = mode.settings.initialPlayer.completedDiaryTasks
        if (completedDiaries.nonEmpty) {
          "Achievement diaries" - {
            completedDiaries.foreach(id =>
              id.toString in (
                cache.diaryTasks.get(id) shouldBe defined
              )
            )
          }
        }

        val completedLeagueTasks = mode.settings.initialPlayer.leagueStatus.completedTasks
        if (completedLeagueTasks.nonEmpty) {
          "League tasks" - {
            completedLeagueTasks.foreach(id =>
              id.toString in (
                cache.leagueTasks.get(id) shouldBe defined
              )
            )
          }
        }

        val completedTiles = mode.settings.initialPlayer.gridStatus.completedTiles
        if (completedTiles.nonEmpty) {
          "Grid tiles" - {
            completedTiles.foreach(id =>
              id.toString in (
                cache.gridTiles.get(id) shouldBe defined
              )
            )
          }
        }
      }
    }
  }
}
