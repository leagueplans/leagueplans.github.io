package com.leagueplans.scraper.telemetry

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.leagueplans.scraper.telemetry.ThresholdLoggerFilter.&&

import scala.compiletime.uninitialized

private object ThresholdLoggerFilter {
  extension (self: FilterReply) {
    private def &&(that: => FilterReply): FilterReply =
      if (self == FilterReply.DENY)
        FilterReply.DENY
      else
        (self, that) match {
          case (_, FilterReply.DENY) => FilterReply.DENY
          case (FilterReply.NEUTRAL, FilterReply.NEUTRAL) => FilterReply.NEUTRAL
          case _ => FilterReply.ACCEPT
        }
  }
}

final class ThresholdLoggerFilter extends Filter[ILoggingEvent] {
  private val thresholdFilter = new ThresholdFilter
  private var logger: String = uninitialized

  def decide(event: ILoggingEvent): FilterReply =
    if (isStarted && event.getLoggerName.startsWith(logger))
      thresholdFilter.decide(event) && FilterReply.ACCEPT
    else
      FilterReply.NEUTRAL

  def setLevel(level: String): Unit =
    thresholdFilter.setLevel(level)

  def setLogger(logger: String): Unit =
    this.logger = logger

  override def start(): Unit =
    if (logger != null) {
      thresholdFilter.start()
      super.start()
    }
}
