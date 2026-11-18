package com.leagueplans.maprender

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

final class ProgressTest extends AnyFreeSpec with Matchers {
  /** A progress whose clock only moves when told to */
  private final class Clock {
    var time: Long = 0
    def advance(seconds: Int): Unit = time += seconds.seconds.toNanos
  }

  "Progress" - {
    "step" - {
      "says nothing until an interval has passed" in {
        val clock = Clock()
        val progress = Progress(100, "regions drawn", interval = 15.seconds, () => clock.time)
        clock.advance(14)
        progress.step() shouldBe None
      }

      "says how far it has got and how long is left, from the pace so far" in {
        val clock = Clock()
        val progress = Progress(100, "regions drawn", interval = 15.seconds, () => clock.time)
        (1 to 24).foreach(_ => progress.step())
        clock.advance(15)
        progress.step() shouldBe Some("25 of 100 regions drawn (25%), about 45s left")
      }

      "waits another interval before saying it again" in {
        val clock = Clock()
        val progress = Progress(100, "regions drawn", interval = 15.seconds, () => clock.time)
        clock.advance(15)
        progress.step() shouldBe defined
        clock.advance(10)
        progress.step() shouldBe None
        clock.advance(5)
        progress.step() shouldBe defined
      }
    }

    "format" - {
      "gives seconds alone under a minute" in {
        Progress.format(45.seconds) shouldBe "45s"
      }

      "gives minutes and two-digit seconds from a minute" in {
        Progress.format(245.seconds) shouldBe "4m05s"
      }
    }
  }
}
