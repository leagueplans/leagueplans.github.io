package com.leagueplans.maprender

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.spi.FilterReply
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class RevisionMismatchFilterTest extends AnyFreeSpec with Matchers {
  private val archiveLogger = "net.runelite.cache.fs.Archive"
  private val revisionMismatch = "revision mismatch for archive {}/{}, expected {} was {}"

  private def event(logger: String, message: String, arguments: Any*): LoggingEvent = {
    val event = LoggingEvent()
    event.setLoggerName(logger)
    event.setMessage(message)
    event.setArgumentArray(arguments.map(_.asInstanceOf[AnyRef]).toArray)
    event
  }

  private val filter = RevisionMismatchFilter()

  "RevisionMismatchFilter" - {
    "drops a revision mismatch where the found revision is the expected one cut to 2 bytes" in {
      // From build 242: 1793635948 & 0xFFFF == 46700
      filter.decide(event(archiveLogger, revisionMismatch, 9, 0, 1793635948, 46700)) shouldBe FilterReply.DENY
    }

    "keeps a revision mismatch that truncation doesn't explain" in {
      filter.decide(event(archiveLogger, revisionMismatch, 9, 0, 1793635948, 46701)) shouldBe FilterReply.NEUTRAL
    }

    "keeps the library's other warnings" in {
      filter.decide(event(archiveLogger, "crc mismatch for archive {}/{}", 9, 0)) shouldBe FilterReply.NEUTRAL
    }

    "keeps the same message from anywhere else" in {
      filter.decide(event("com.leagueplans.maprender.Main", revisionMismatch, 9, 0, 1793635948, 46700)) shouldBe
        FilterReply.NEUTRAL
    }
  }
}
