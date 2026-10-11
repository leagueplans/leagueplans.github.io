package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata, SchemaVersion}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js.Date

final class PlanSwitcherTest extends AnyFreeSpec with Matchers {
  "PlanSwitcher" - {
    "listing the other plans" - {
      def metadata(name: String, timestamp: Double): PlanMetadata =
        PlanMetadata(name, new Date(timestamp), SchemaVersion.values.last)

      val open = PlanID.fromString("open")
      val older = PlanID.fromString("older")
      val newer = PlanID.fromString("newer")
      val plans = Map(
        older -> metadata("Older", 1000),
        open -> metadata("Open", 3000),
        newer -> metadata("Newer", 2000)
      )

      "leaves out the open plan" in {
        PlanSwitcher.otherPlans(plans, open).map(_._1) should not contain open
      }

      "puts the most recently edited first" in {
        PlanSwitcher.otherPlans(plans, open).map(_._1) shouldEqual List(newer, older)
      }

      "orders plans edited at the same time by name" in {
        val tied = Map(
          newer -> metadata("B", 1000),
          older -> metadata("A", 1000)
        )
        PlanSwitcher.otherPlans(tied, open).map(_._1) shouldEqual List(older, newer)
      }

      "is empty when there are no other plans" in {
        PlanSwitcher.otherPlans(Map(open -> metadata("Open", 1)), open) shouldEqual List.empty
      }
    }
  }
}
