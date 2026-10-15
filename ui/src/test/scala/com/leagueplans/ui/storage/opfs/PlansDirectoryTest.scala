package com.leagueplans.ui.storage.opfs

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.storage.model.PlanMetadata
import com.leagueplans.uicommon.wrappers.opfs.MockDirectoryHandle
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, OptionValues}

import scala.util.Using

final class PlansDirectoryTest extends AnyFreeSpec with Matchers with EitherValues with OptionValues {
  private val plan = Plan("Plan", Forest.empty, Armageddon.settings)

  /** The mock file system completes every operation as soon as it's asked to */
  private def run[T](stream: => EventStream[T]): T =
    Using(new ManualOwner) { owner =>
      var result = Option.empty[T]
      stream.foreach(value => result = Some(value))(using owner)
      result.value
    }(using _.killSubscriptions()).get

  "PlansDirectory" - {
    "listPlans" - {
      "lists plans whose metadata can't be read apart from the others" in {
        val root = new MockDirectoryHandle
        val directory = PlansDirectory(root)
        val readable = run(directory.create(PlanMetadata("Readable"), plan)).value
        val unreadable = run(directory.create(PlanMetadata("Unreadable"), plan)).value
        val unreadableHandle = run(root.getSubDirectory(unreadable)).value.value
        run(unreadableHandle.replaceFileContent("metadata.bin", "Not metadata")).value

        val plans = run(directory.listPlans()).value
        plans.readable.keySet shouldEqual Set(readable)
        plans.unreadable shouldEqual Set(unreadable)
      }
    }
  }
}
