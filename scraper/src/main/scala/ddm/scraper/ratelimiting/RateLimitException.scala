package ddm.scraper.ratelimiting

import ddm.scraper.ratelimiting.RateLimiter.Error

final class RateLimitException(cause: RateLimiter.Error) extends RuntimeException(
  cause match {
    case RateLimiter.Error.FailedToBuffer =>
      "Could not buffer the request for rate limiting."
    case RateLimiter.Error.Shutdown =>
      "Rate limiter has been shutdown. No further requests can be made."
  }
)
