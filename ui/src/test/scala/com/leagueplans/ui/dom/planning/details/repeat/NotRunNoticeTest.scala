package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{ExpTarget, Repetitions, Step, StepDetails, StopCondition}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.projection.model.{LoopRun, StepLoop}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class NotRunNoticeTest extends AnyFreeSpec with Matchers {
  "NotRunNotice" - {
    val until = Repetitions.Until(StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level(60)), stopBefore = false), None)

    def step(id: String, repetitions: Repetitions): Step =
      Step(Step.ID.fromString(id), StepDetails(id).copy(repetitions = repetitions))

    def loop(reps: Int*): StepLoop =
      StepLoop(until, reps.toList.map(n => LoopRun(n, LoopRun.Outcome.AlreadyMet(List.empty), List.empty)))

    val inner = step("inner", until)
    val outer = step("outer", until)

    "names an ancestor that repeats until a condition and played no reps" in {
      NotRunNotice.cause(List(inner, outer), Map(inner.id -> loop(5), outer.id -> loop(0))) shouldBe Some(outer)
    }

    "names the outermost such ancestor, since that's why the others don't run either" in {
      NotRunNotice.cause(List(inner, outer), Map(inner.id -> loop(0), outer.id -> loop(0))) shouldBe Some(outer)
    }

    "says nothing while an ancestor plays reps on any of its passes" in {
      NotRunNotice.cause(List(inner), Map(inner.id -> loop(0, 3))) shouldBe None
    }

    "says nothing for an ancestor whose passes aren't known yet" in {
      NotRunNotice.cause(List(inner), Map.empty) shouldBe None
    }

    "says nothing for an ancestor that repeats a fixed number of times, whatever was last worked out" in {
      val fixed = step("fixed", Repetitions.Fixed(3))
      NotRunNotice.cause(List(fixed), Map(fixed.id -> loop(0))) shouldBe None
    }
  }
}
