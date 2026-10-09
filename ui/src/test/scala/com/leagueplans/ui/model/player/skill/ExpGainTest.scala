package com.leagueplans.ui.model.player.skill

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.player.skill.ExpGain.{Draft, Problem, TargetKind}
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ExpGainTest extends AnyFreeSpec with Matchers with EitherValues {
  private def gain(
    actions: String = "1",
    each: String = "",
    target: String = "",
    kind: TargetKind = TargetKind.Level,
    current: Exp = Exp(0),
    multiplier: Double = 1
  ): Either[Problem, ExpGain.Gain] =
    ExpGain(Draft(actions, each, target, kind), Skill.Woodcutting, current, multiplier)

  "ExpGain" - {
    "multiplies actions by the exp each gives, keeping tenths" in {
      val result = gain(actions = "3", each = "12.5").value
      result.base shouldBe Exp(37.5)
      (result.actions, result.expEach) shouldBe (3, Some(Exp(12.5)))
    }

    "is a single action when no actions are typed" in {
      gain(each = "25").value.actions shouldBe 1
    }

    "previews the exp after the multiplier, and the level it ends on" in {
      val result = gain(each = "100", multiplier = 2).value
      (result.gained, result.from, result.to) shouldBe (Exp(200), Level.L1, Level.L3)
    }

    "is incomplete until there's exp each or a target" in {
      gain() shouldBe Left(Problem.Incomplete)
      gain(actions = "40") shouldBe Left(Problem.Incomplete)
    }

    "explains what can't be used" in {
      gain(each = "nothing") shouldBe a[Left[Problem.Invalid, ?]]
      gain(actions = "0", each = "25") shouldBe a[Left[Problem.Invalid, ?]]
      gain(target = "30.5") shouldBe a[Left[Problem.Invalid, ?]]
    }

    "with a target level" - {
      "counts the actions that reach it, with the multiplier" in {
        // 1,154 exp to level 10 at 10 exp each and 5× is 23.08 actions
        val result = gain(each = "10", target = "10", multiplier = 5).value
        result.actionsToTarget shouldBe Some(24)
        result.base shouldBe Exp(240)
        (result.actions, result.expEach) shouldBe (24, Some(Exp(10)))
      }

      "counts exactly when the exp divides evenly" in {
        // 83 exp to level 2
        gain(each = "83", target = "2").value.actionsToTarget shouldBe Some(1)
      }

      "takes the target over the actions typed" in {
        gain(actions = "500", each = "83", target = "2").value.base shouldBe Exp(83)
      }

      "gains exactly the exp that reaches it when there's no exp each, rounded up" in {
        // Level 10 needs 1,154 exp
        gain(target = "10", current = Exp(154)).value.base shouldBe Exp(1000)
        gain(target = "10", current = Exp(154), multiplier = 5).value.base shouldBe Exp(200)
        val result = gain(target = "2", multiplier = 3).value
        result.gained.raw should be >= Exp(83).raw
      }

      "refuses a level that's already reached, or past 99" in {
        gain(target = "5", current = Exp(500)) shouldBe a[Left[Problem.Invalid, ?]]
        gain(target = "100") shouldBe a[Left[Problem.Invalid, ?]]
      }
    }

    "with a target in exp" - {
      "counts the actions that reach it" in {
        gain(each = "25", target = "1000", kind = TargetKind.Exp).value.actionsToTarget shouldBe Some(40)
      }

      "refuses exp that's already reached, or past 200M" in {
        gain(target = "100", kind = TargetKind.Exp, current = Exp(100)) shouldBe a[Left[Problem.Invalid, ?]]
        gain(target = "200000001", kind = TargetKind.Exp) shouldBe a[Left[Problem.Invalid, ?]]
      }

      "reaches 200M, though rounding up would pass it" in {
        val result = gain(target = "200000000", kind = TargetKind.Exp, multiplier = 3).value
        result.gained shouldBe Exp.max
      }
    }

    "refuses a gain that would pass 200M exp" in {
      gain(actions = "1000", each = "1000000") shouldBe a[Left[Problem.Invalid, ?]]
      gain(each = "150000000", current = Exp(100000000)) shouldBe a[Left[Problem.Invalid, ?]]
    }
  }
}
