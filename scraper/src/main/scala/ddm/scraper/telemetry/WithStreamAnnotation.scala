package ddm.scraper.telemetry

import zio.metrics.MetricLabel
import zio.stream.{ZStream, ZStreamAspect}
import zio.{LogAnnotation, Trace}

type WithStreamAnnotation = ZStreamAspect[Nothing, Any, Nothing, Any, Nothing, Any]

object WithStreamAnnotation {
  def apply(tags: (String, String)*): WithStreamAnnotation =
    forLogs(tags*) @@ forMetrics(tags*)

  def forLogs(tags: (String, String)*): WithStreamAnnotation =
    new ZStreamAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      def apply[
        R >: Nothing <: Any,
        E >: Nothing <: Any,
        A >: Nothing <: Any
      ](stream: ZStream[R, E, A])(using Trace): ZStream[R, E, A] =
        ZStream.logAnnotate(tags.map((k, v) => LogAnnotation(k, v)).toSet) *> stream
    }

  def forMetrics(tags: (String, String)*): WithStreamAnnotation =
    new ZStreamAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      def apply[
        R >: Nothing <: Any,
        E >: Nothing <: Any,
        A >: Nothing <: Any
      ](stream: ZStream[R, E, A])(using Trace): ZStream[R, E, A] =
        ZStream.tagged(tags.map((k, v) => MetricLabel(k, v)).toSet) *> stream
    }
}
