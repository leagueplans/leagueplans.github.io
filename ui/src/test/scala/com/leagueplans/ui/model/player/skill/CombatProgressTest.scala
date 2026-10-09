package com.leagueplans.ui.model.player.skill

import com.leagueplans.common.model.Skill
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class CombatProgressTest extends AnyFreeSpec with Matchers {
  private def stats(levels: (Skill, Int)*): Stats =
    Stats(levels.map((skill, level) => skill -> Level(level).bound)*)

  "CombatProgress" - {
    "counts the levels each combat skill needs to raise the combat level, fewest first" in {
      // A fresh account: combat 3.4, from Hitpoints 10
      val fresh = stats(Skill.Hitpoints -> 10)
      val levels = CombatProgress.levelsToNext(fresh).toMap
      // 13/40 per melee level, so two Attack levels reach 4.05
      levels(Skill.Attack) shouldBe 2
      levels(Skill.Strength) shouldBe 2
      // A quarter per Defence or Hitpoints level, so three reach 4.15
      levels(Skill.Defence) shouldBe 3
      levels(Skill.Hitpoints) shouldBe 3
      // Prayer counts half its level at a quarter, rounded down, so it takes level 6
      levels(Skill.Prayer) shouldBe 5
      CombatProgress.levelsToNext(fresh).head._2 shouldBe 2
    }

    "counts the levels ranged needs to overtake melee, since only the strongest style counts" in {
      // Ranged counts 1.5 times its level, so with Attack and Strength at 40 it needs level 54
      val melee = stats(Skill.Hitpoints -> 10, Skill.Attack -> 40, Skill.Strength -> 40)
      CombatProgress.levelsToNext(melee).toMap.get(Skill.Ranged) shouldBe Some(54 - 1)
    }

    "has nothing to gain at the highest combat level" in {
      val maxed = stats(Skill.combats.toList.map(_ -> 99)*)
      CombatProgress.levelsToNext(maxed) shouldBe List.empty
    }
  }
}
