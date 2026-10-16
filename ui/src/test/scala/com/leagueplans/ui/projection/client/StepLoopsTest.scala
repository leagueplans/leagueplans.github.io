package com.leagueplans.ui.projection.client

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{ExpTarget, Repetitions, Step, StepDetails, StopCondition}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.projection.client.StepLoops.Known
import com.leagueplans.ui.projection.model.{LoopRun, StepLoop}
import com.raquo.airstream.ownership.ManualOwner
import com.raquo.airstream.state.Var
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class StepLoopsTest extends AnyFreeSpec with Matchers {
  "StepLoops" - {
    def until(level: Int): Repetitions =
      Repetitions.Until(StopCondition.SkillReaches(Skill.Hunter, ExpTarget.AtLevel(Level(level)), stopBefore = false), None)

    val step = Step(Step.ID.fromString("step"), StepDetails("step").copy(repetitions = until(60)))
    val loop = StepLoop(until(60), List(LoopRun(412, LoopRun.Outcome.Met(List.empty), List.empty)))

    def test(f: (Var[Step], Var[Map[Step.ID, StepLoop]]) => ManualOwner ?=> Assertion): Assertion = {
      val stepVar = Var(step)
      val all = Var(Map(step.id -> loop))
      Using(new ManualOwner)(f(stepVar, all)(using _))(using _.killSubscriptions()).get
    }

    "gives a step's passes as current while it repeats as they were worked out for" in test { (stepVar, all) =>
      StepLoops(all.signal).get(stepVar.signal).observe.now() shouldBe Some(Known(loop, current = true))
    }

    "keeps a step's last passes, marked stale, until they're worked out for a change to it" in test { (stepVar, all) =>
      val known = StepLoops(all.signal).get(stepVar.signal).observe
      stepVar.update(_.deepCopy(repetitions = until(70)))
      known.now() shouldBe Some(Known(loop, current = false))
      val updated = StepLoop(until(70), List(LoopRun(600, LoopRun.Outcome.Met(List.empty), List.empty)))
      all.set(Map(step.id -> updated))
      known.now() shouldBe Some(Known(updated, current = true))
    }

    "has nothing for a step whose passes have never been worked out" in test { (stepVar, all) =>
      all.set(Map.empty)
      StepLoops(all.signal).get(stepVar.signal).observe.now() shouldBe None
    }
  }
}
