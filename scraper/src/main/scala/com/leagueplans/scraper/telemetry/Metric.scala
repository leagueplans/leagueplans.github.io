package com.leagueplans.scraper.telemetry

import zio.{UIO, ZIO}
import zio.metrics.Metric as ZMetric

object Metric {
  type Counter[-In] = ZMetric.Counter[In]
  type Gauge[-In] = ZMetric.Gauge[In]

  def makeCounter(name: String): UIO[Counter[Long]] =
    make(ZMetric.counter(name))

  def makeCounterT[In](name: String)(f: In => Long): UIO[Counter[In]] =
    makeCounter(name).map(_.contramap(f))

  def makeGauge(name: String): UIO[Gauge[Double]] =
    make(ZMetric.gauge(name))

  def makeGaugeT[In](name: String)(f: In => Double): UIO[Gauge[In]] =
    makeGauge(name).map(_.contramap(f))

  private def make[Type, In, Out](build: => ZMetric[Type, In, Out]): UIO[ZMetric[Type, In, Out]] =
    ZIO.tags.map(build.tagged)
}
