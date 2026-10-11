package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.model.status.StatusTracker.Status
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata, SchemaVersion}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.scalajs.js.Date

final class PlanManagerTest extends AnyFreeSpec with Matchers {
  private val older = PlanID.fromString("older")
  private val newer = PlanID.fromString("newer")
  private val plans = Map(
    older -> PlanMetadata("Older", new Date(1000), SchemaVersion.values.last),
    newer -> PlanMetadata("Newer", new Date(2000), SchemaVersion.values.last)
  )
  
  "PlanManager" - {
    "choosing the plan to open at startup" - {
      "picks the plan last open in this browser" in {
        PlanManager.choose(plans, lastOpen = Some(older)) shouldEqual Some(older)
      }

      "picks the most recently changed plan if none was open" in {
        PlanManager.choose(plans, lastOpen = None) shouldEqual Some(newer)
      }

      "picks the most recently changed plan if the last one no longer exists" in {
        PlanManager.choose(plans, lastOpen = Some(PlanID.fromString("deleted"))) shouldEqual Some(newer)
      }

      "picks nothing when there are no plans" in {
        PlanManager.choose(Map.empty, lastOpen = Some(older)) shouldEqual None
      }
    }

    "combining statuses" - {
      val problem = Status.Failed("broken")

      "shows a problem over anything else" in {
        PlanManager.worst(Status.Busy, problem) shouldEqual problem
        PlanManager.worst(problem, Status.Idle) shouldEqual problem
      }

      "shows work in progress over idleness" in {
        PlanManager.worst(Status.Idle, Status.Busy) shouldEqual Status.Busy
        PlanManager.worst(Status.Busy, Status.Idle) shouldEqual Status.Busy
      }

      "is idle when both are" in {
        PlanManager.worst(Status.Idle, Status.Idle) shouldEqual Status.Idle
      }
    }
  }
}
