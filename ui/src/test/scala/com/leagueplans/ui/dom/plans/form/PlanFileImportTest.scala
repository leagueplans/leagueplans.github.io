package com.leagueplans.ui.dom.plans.form

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class PlanFileImportTest extends AnyFreeSpec with Matchers {
  "PlanFileImport" - {
    "isPlanFileName" - {
      "should accept the names the file picker accepts" in {
        List("Route.plan", "Route.plan.gz", "route.PLAN", "Route.gzip").foreach(
          PlanFileImport.isPlanFileName(_) shouldBe true
        )
      }

      "should refuse other files" in {
        List("Route.plan.txt", "notes.txt", "Route", ".DS_Store").foreach(
          PlanFileImport.isPlanFileName(_) shouldBe false
        )
      }
    }
  }
}
