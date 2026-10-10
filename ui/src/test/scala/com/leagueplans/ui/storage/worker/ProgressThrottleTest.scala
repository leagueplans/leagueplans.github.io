package com.leagueplans.ui.storage.worker

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ProgressThrottleTest extends AnyFreeSpec with Matchers {
  "ProgressThrottle" - {
    "always allows the first report" in {
      ProgressThrottle(100, () => 0).allow() shouldBe true
    }

    "blocks reports until the interval has passed since the last one allowed" in {
      var time = 0.0
      val throttle = ProgressThrottle(100, () => time)
      throttle.allow() shouldBe true

      time = 99
      throttle.allow() shouldBe false

      time = 100
      throttle.allow() shouldBe true

      time = 150
      throttle.allow() shouldBe false
    }
  }
}
