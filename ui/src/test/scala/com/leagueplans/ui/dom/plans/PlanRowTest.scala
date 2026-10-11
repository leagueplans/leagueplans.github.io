package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata, SchemaVersion}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js.Date

final class PlanRowTest extends AnyFreeSpec with Matchers {
  // Months count from zero, as they do in JavaScript
  private val now = new Date(2026, 9, 10, 22, 30)
  "PlanRow" - {
    "formatting when a plan was edited" - {
      "says today, with the time" in {
        PlanRow.formatEdited(new Date(2026, 9, 10, 21, 14), now) shouldEqual "today, 21:14"
      }

      "pads the hours and minutes" in {
        PlanRow.formatEdited(new Date(2026, 9, 10, 0, 5), now) shouldEqual "today, 00:05"
      }

      "says yesterday for any time on the previous calendar day" in {
        PlanRow.formatEdited(new Date(2026, 9, 9, 9, 2), now) shouldEqual "yesterday, 09:02"
        PlanRow.formatEdited(new Date(2026, 9, 9, 23, 59), now) shouldEqual "yesterday, 23:59"
      }

      "gives the date for anything earlier" in {
        PlanRow.formatEdited(new Date(2026, 9, 2, 12, 0), now) shouldEqual "2 Oct 2026"
        PlanRow.formatEdited(new Date(2025, 10, 12, 8, 0), now) shouldEqual "12 Nov 2025"
      }

      "counts days by the calendar, not by hours elapsed" in {
        PlanRow.formatEdited(new Date(2026, 9, 9, 23, 59), new Date(2026, 9, 10, 0, 1)) shouldEqual
          "yesterday, 23:59"
      }

      "treats a time in the future as today" in {
        PlanRow.formatEdited(new Date(2026, 9, 11, 1, 0), now) shouldEqual "today, 01:00"
      }
    }

    "listing plans newest first" - {
      "puts the most recently edited first, whatever the order they were given in" in {
        val old = PlanID.fromString("old")
        val recent = PlanID.fromString("recent")
        val plans = Map(
          old -> PlanMetadata("Old", new Date(1000), SchemaVersion.values.last),
          recent -> PlanMetadata("Recent", new Date(2000), SchemaVersion.values.last)
        )
        PlanRow.newestFirst(plans).map(_._1) shouldEqual List(recent, old)
      }

      "is empty without plans" in {
        PlanRow.newestFirst(Map.empty) shouldEqual List.empty
      }
    }
  }
}
