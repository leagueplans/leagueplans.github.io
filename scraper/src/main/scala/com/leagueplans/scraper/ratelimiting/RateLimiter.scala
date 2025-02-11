package com.leagueplans.scraper.ratelimiting

import zio.{IO, Trace}

object RateLimiter {
  enum Error {
    case FailedToBuffer, Shutdown

    def exception(): RateLimitException =
      RateLimitException(this)
  }
}

trait RateLimiter {
  def await(using Trace): IO[RateLimiter.Error, Unit]
}
