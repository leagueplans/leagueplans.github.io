package com.leagueplans.ui.dom.planning

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class PlanningTipsTest extends AnyFreeSpec with Matchers {
  "PlanningTips" - {
    "the tip about adding steps" - {
      "waits for the first effect" in {
        PlanningTips.showNextStepsTip(stepCount = 1, hasEffects = false) shouldBe false
        PlanningTips.showNextStepsTip(stepCount = 1, hasEffects = true) shouldBe true
      }

      "shows while the plan is small" in {
        PlanningTips.showNextStepsTip(PlanningTips.nextStepsTipLimit, hasEffects = true) shouldBe true
      }

      "stops showing once the plan has grown" in {
        PlanningTips.showNextStepsTip(PlanningTips.nextStepsTipLimit + 1, hasEffects = true) shouldBe false
      }

      "doesn't show for an empty plan" in {
        PlanningTips.showNextStepsTip(stepCount = 0, hasEffects = false) shouldBe false
      }
    }
  }
}
