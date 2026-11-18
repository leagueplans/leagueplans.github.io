package com.leagueplans.maprender

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

object RevisionMismatchFilter {
  private val logger = "net.runelite.cache.fs.Archive"
  private val message = "revision mismatch for archive {}/{}, expected {} was {}"
}

/** Drops the cache library's false "revision mismatch" warnings, so that a render's log stays
  * readable.
  *
  * The cache records each archive's revision twice: as 4 bytes in its index, and as 2 bytes at
  * the end of the archive's own data. The library warns whenever the two differ, without
  * allowing for the second holding only the low 2 bytes of the first. So every archive with a
  * revision above 65,535 warns, although nothing is wrong. In current caches that's almost
  * all of them: 4,024 warnings for build 242, against a dozen lines of progress.
  *
  * Only those warnings are dropped. A mismatch the truncation doesn't explain is still logged,
  * as is any other warning.
  */
final class RevisionMismatchFilter extends Filter[ILoggingEvent] {
  import RevisionMismatchFilter.*

  def decide(event: ILoggingEvent): FilterReply =
    if (event.getLoggerName == logger && event.getMessage == message && isTruncation(event))
      FilterReply.DENY
    else
      FilterReply.NEUTRAL

  /** Whether the revision the library found is the one it expected, cut to 2 bytes */
  private def isTruncation(event: ILoggingEvent): Boolean =
    Option(event.getArgumentArray).exists {
      case Array(_, _, expected: Integer, found: Integer) => (expected.intValue & 0xFFFF) == found.intValue
      case _ => false
    }
}
