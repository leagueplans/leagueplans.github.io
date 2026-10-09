package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{Effect, ExpTarget}
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ExpDetailTest extends AnyFreeSpec with Matchers {
  private val gain = Effect.GainExp(Skill.Fishing, 1, Exp(25))

  "ExpDetail" - {
    "changes the actions or the exp each, keeping the other" in {
      ExpDetail.withActions(gain, "40") shouldBe Right(Effect.GainExp(Skill.Fishing, 40, Exp(25)))
      ExpDetail.withExpEach(gain, "30") shouldBe Right(Effect.GainExp(Skill.Fishing, 1, Exp(30)))
    }

    "refuses actions that would come to more exp than an effect holds" in {
      ExpDetail.withActions(gain, "10000000").isLeft shouldBe true
      ExpDetail.withActions(gain, "0").isLeft shouldBe true
    }

    "changes a target's exp each, keeping the target" in {
      val toTarget: Effect.GainExpToTarget = Effect.GainExpToTarget(Skill.Fishing, ExpTarget.AtLevel(Level(30)), Some(Exp(25)))
      ExpDetail.withTargetExpEach(toTarget, "40") shouldBe Right(toTarget.copy(expEach = Some(Exp(40))))
    }
  }
}
