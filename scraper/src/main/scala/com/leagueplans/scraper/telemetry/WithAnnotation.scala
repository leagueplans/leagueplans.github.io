package com.leagueplans.scraper.telemetry

import zio.ZIOAspect

type WithAnnotation = ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any]

object WithAnnotation {
  def apply(tags: (String, String)*): WithAnnotation =
    forLogs(tags*) @@ forMetrics(tags*)

  def forLogs(tags: (String, String)*): WithAnnotation =
    ZIOAspect.annotated(tags*)

  def forMetrics(tags: (String, String)*): WithAnnotation =
    ZIOAspect.tagged(tags*)
}
