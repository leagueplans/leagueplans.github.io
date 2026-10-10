package com.leagueplans.ui.storage.opfs

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.storage.model.{PlanMetadata, Rename, StepUpdates}
import com.leagueplans.uicommon.wrappers.opfs.MockDirectoryHandle
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}

import scala.collection.mutable
import scala.util.Using

final class PlanDirectoryTest extends AnyFreeSpec with Matchers with EitherValues with OptionValues {
  private val steps = List(Step("1"), Step("2"), Step("3"))

  private val plan = Plan(
    "Original",
    Forest.from(steps.map(step => step.id -> step).toMap, Map.empty, steps.map(_.id)),
    Armageddon.settings
  )

  /** The mock file system completes every operation as soon as it's asked to */
  private def run[T](stream: => EventStream[T]): T =
    Using(new ManualOwner) { owner =>
      var result = Option.empty[T]
      stream.foreach(value => result = Some(value))(using owner)
      result.value
    }(using _.killSubscriptions()).get

  private def createDirectory(): PlanDirectory[MockDirectoryHandle] = {
    val directory = PlanDirectory(new MockDirectoryHandle)
    run(directory.create(PlanMetadata(plan.name), plan)).value
    directory
  }

  "PlanDirectory" - {
    "readPlan" - {
      "reports the number of steps before reading any, then as each is read" in {
        val directory = createDirectory()
        val progress = mutable.ListBuffer.empty[(Int, Int)]

        run(directory.readPlan((read, total) => progress += read -> total)).value
        progress.toList shouldEqual List(0 -> 3, 1 -> 3, 2 -> 3, 3 -> 3)
      }
    }

    "applyUpdate" - {
      "renames the plan" in {
        val directory = createDirectory()

        run(directory.applyUpdate(Rename("Renamed"))).value
        run(directory.readMetadata()).value.name shouldEqual "Renamed"
        run(directory.readPlan((_, _) => ())).value.steps.nodes shouldEqual plan.steps.nodes
      }

      "keeps the plan's name when it's another kind of update" in {
        val directory = createDirectory()

        run(directory.applyUpdate(StepUpdates(List.empty))).value
        run(directory.applyUpdate(Armageddon.settings)).value
        run(directory.readMetadata()).value.name shouldEqual "Original"
      }
    }
  }
}
