package com.leagueplans.ui.model.player.skill

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class LevelProgressTest extends AnyFreeSpec with Matchers {
  "LevelProgress" - {
    "is the fraction of the way through the current level" in {
      // Level 2 runs from 83 to 174 exp
      val progress = LevelProgress(Exp(128.5), was = Exp(128.5))
      progress.level shouldBe Level.L2
      progress.after shouldBe 0.5 +- 0.001
      progress.before shouldBe progress.after
      progress.toNext shouldBe Exp(45.5)
    }

    "marks what the step added within the same level" in {
      val progress = LevelProgress(Exp(150), was = Exp(100))
      progress.before should be < progress.after
      progress.gained shouldBe Exp(50)
      progress.levelsGained shouldBe 0
    }

    "counts the whole fill as gained when the step gained a level" in {
      val progress = LevelProgress(Exp(200), was = Exp(50))
      progress.level shouldBe Level.L3
      progress.before shouldBe 0
      progress.levelsGained shouldBe 2
    }

    "is full at level 99, with nothing to the next level" in {
      val progress = LevelProgress(Exp(13034431), was = Exp(13034431))
      progress.isMaxed shouldBe true
      progress.after shouldBe 1
      progress.toNext shouldBe Exp(0)
    }

    "shortens large amounts of exp" in {
      LevelProgress.shorten(Exp(127)) shouldBe "127"
      LevelProgress.shorten(Exp(45210)) shouldBe "45,210"
      LevelProgress.shorten(Exp(245800)) shouldBe "245K"
      LevelProgress.shorten(Exp(1234567)) shouldBe "1.23M"
    }

    "rounds a fraction of exp up, so it never reads as 0 to go" in {
      LevelProgress.shorten(Exp.tenths(3)) shouldBe "1"
    }
  }
}
