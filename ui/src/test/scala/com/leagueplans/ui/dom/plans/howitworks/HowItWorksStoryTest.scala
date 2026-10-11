package com.leagueplans.ui.dom.plans.howitworks

import com.leagueplans.common.model.{GridTile, Item, LeagueTask, Skill}
import com.leagueplans.ui.dom.plans.howitworks.HowItWorksStory.*
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.player.{Cache, Player, Quest}
import com.leagueplans.ui.testutils.readFile
import io.circe.Decoder
import io.circe.parser.decode
import org.scalatest.TryValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

/** Plays the welcome screen's story against the real game data, so that a change to the data or to
  * how the planner works shows up here, rather than as a story that no longer adds up */
final class HowItWorksStoryTest extends AnyFreeSpec with Matchers with TryValues {
  private lazy val cache: Cache =
    Cache(
      readData[Set[Item]]("items"),
      readData[Set[Quest]]("quests"),
      readData[Set[DiaryTask]]("diaryTasks"),
      readData[Set[LeagueTask]]("leagueTasks"),
      readData[Set[GridTile]]("gridMaster")
    )

  private def readData[T : Decoder](fileName: String): T =
    readFile(s"ui/src/main/web/data/$fileName.json").flatMap(bytes => decode[T](String(bytes)).toTry).success.value

  private lazy val scenes = HowItWorksStory.scenes(cache)

  private def level(player: Player, skill: Skill): Int = Level.of(player.stats(skill)).raw
  private def held(player: Player, item: Item.ID): Int = player.get(Depository.Kind.Inventory).count(item, noted = false)

  "HowItWorksStory" - {
    "The story's items should be the ones it's about" in {
      cache.items(bronzeAxe).name shouldBe "Bronze axe"
      cache.items(logs).name shouldBe "Logs"
      cache.items(tinderbox).name shouldBe "Tinderbox"
    }

    "Its beats should be in order, within the loop" in {
      val times = beats.map(_.at)
      times shouldBe times.sorted
      times.last should be < loopMillis
    }

    "Cutting the logs should fill the inventory and train Woodcutting, a log at a time" in {
      val cutting = scenes.filter(scene => scene.beat.steps(scene.beat.focus) == chop && scene.beat.played > 0)
      cutting.map(scene => held(scene.shown, logs)) shouldBe (1 to 10).toList
      level(cutting.last.shown, Skill.Woodcutting) shouldBe 3
    }

    "Burning the logs should need a tinderbox, until one's picked up" in {
      val mistake = scenes.find(_.beat.spotlight).get
      mistake.problems.values.flatten.toList shouldBe List("Tinderbox isn't in the inventory at the start of this step")

      // Not until what it needs is listed
      val written = scenes.filter(scene => scene.beat.steps.contains(burn) && scene.beat.rowsShown == 0)
      written should not be empty
      all(written.map(_.problems)) shouldBe empty

      val fixed = scenes.filter(_.beat.steps.contains(pickUpTinderbox))
      all(fixed.map(_.problems)) shouldBe empty
    }

    "The story should end with the logs burnt, and Firemaking trained" in {
      val end = scenes.last.shown
      held(end, logs) shouldBe 0
      held(end, bronzeAxe) shouldBe 1
      held(end, tinderbox) shouldBe 1
      level(end, Skill.Woodcutting) shouldBe 3
      level(end, Skill.Firemaking) shouldBe 5
    }

    "Items should keep their slots as others come and go" in {
      scenes.find(_.slots.contains(Some(tinderbox))).get.slots.flatten shouldBe List(bronzeAxe) ++ List.fill(10)(logs) :+ tinderbox
      scenes.last.slots shouldBe Some(bronzeAxe) +: Vector.fill(10)(None) :+ Some(tinderbox)
    }
  }
}
