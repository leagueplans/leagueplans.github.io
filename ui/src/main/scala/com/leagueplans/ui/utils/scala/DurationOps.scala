package com.leagueplans.ui.utils.scala

import scala.concurrent.duration.{Duration, DurationDouble, FiniteDuration}

object DurationOps {
  private val timeCap: FiniteDuration = 365.25.days
  
  extension (self: Duration) {
    def safeAdd(other: Duration): Duration =
      if (!self.isFinite || !other.isFinite)
        Duration.Inf
      else
        try {
          val result = self + other
          if (result >= timeCap) Duration.Inf else result
        } catch { case _: IllegalArgumentException =>
          Duration.Inf
        }

    def safeMul(n: Int): Duration =
      if (!self.isFinite)
        self
      else
        try {
          val result = self * n
          if (result >= timeCap) Duration.Inf else result
        } catch { case _: IllegalArgumentException =>
          Duration.Inf
        }
  }
}
