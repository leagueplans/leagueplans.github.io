package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.model.plan.Duration
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class DurationTextTest extends AnyFreeSpec with Matchers {
  "DurationText.parse" - {
    "reads a bare number as seconds" in {
      DurationText.parse("90") shouldBe Right(Duration.seconds(90))
    }

    "reads hours, minutes and seconds in any combination" in {
      DurationText.parse("2m30s") shouldBe Right(Duration.seconds(150))
      DurationText.parse("1h 5m") shouldBe Right(Duration.seconds(3900))
      DurationText.parse(" 1H2M3S ") shouldBe Right(Duration.seconds(3723))
      DurationText.parse("45s") shouldBe Right(Duration.seconds(45))
    }

    "reads ticks" in {
      DurationText.parse("50t") shouldBe Right(Duration.ticks(50))
      DurationText.parse("50 ticks") shouldBe Right(Duration.ticks(50))
      DurationText.parse("1 tick") shouldBe Right(Duration.ticks(1))
    }

    "treats an empty box as no duration" in {
      DurationText.parse("") shouldBe Right(Duration.seconds(0))
      DurationText.parse("0") shouldBe Right(Duration.seconds(0))
    }

    "rejects text it can't read" in {
      DurationText.parse("soon").isLeft shouldBe true
      DurationText.parse("2m 30").isLeft shouldBe true
      DurationText.parse("2m 3m").isLeft shouldBe true
      DurationText.parse("50t 2s").isLeft shouldBe true
    }

    "rejects durations past the limits" in {
      DurationText.parse("6h") shouldBe Right(Duration.seconds(DurationText.maxSeconds))
      DurationText.parse("6h 1s").isLeft shouldBe true
      DurationText.parse("99999999999999999999").isLeft shouldBe true
      DurationText.parse(s"${DurationText.maxTicks + 1}t").isLeft shouldBe true
    }
  }

  "DurationText.format" - {
    "writes seconds with the largest units first, leaving out zeros" in {
      DurationText.format(Duration.seconds(3723)) shouldBe "1h 2m 3s"
      DurationText.format(Duration.seconds(3600)) shouldBe "1h"
      DurationText.format(Duration.seconds(150)) shouldBe "2m 30s"
      DurationText.format(Duration.seconds(0)) shouldBe "0s"
    }

    "writes ticks as ticks" in {
      DurationText.format(Duration.ticks(50)) shouldBe "50t"
    }

    "writes text that parses back to the same duration" in {
      List(Duration.seconds(3723), Duration.seconds(59), Duration.ticks(7), Duration.seconds(21600)).foreach(d =>
        DurationText.parse(DurationText.format(d)) shouldBe Right(d)
      )
    }
  }
}
