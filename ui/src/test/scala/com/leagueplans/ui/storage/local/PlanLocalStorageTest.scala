package com.leagueplans.ui.storage.local

import com.leagueplans.ui.storage.model.PlanID
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class PlanLocalStorageTest extends AnyFreeSpec with Matchers {
  private val planID = PlanID.fromString("3f0c2a")

  "keys start with the plan, then say what they hold" - {
    "collapsed steps" in {
      PlanLocalStorage.Key.CollapsedSteps.of(planID) shouldEqual "3f0c2a-collapsed-steps"
    }

    "the selected section" in {
      PlanLocalStorage.Key.SelectedSection.of(planID) shouldEqual "3f0c2a-selected-section"
    }
  }

  // Tests run without local storage, which is one of the cases it must survive
  "does nothing, rather than failing, without local storage" in {
    val storage = PlanLocalStorage(planID)
    storage.set(PlanLocalStorage.Key.CollapsedSteps, "a,b")
    storage.get(PlanLocalStorage.Key.CollapsedSteps) shouldEqual None
    PlanLocalStorage.forget(planID)
  }
}
