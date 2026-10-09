package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.skill.Exp
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
  }
}
