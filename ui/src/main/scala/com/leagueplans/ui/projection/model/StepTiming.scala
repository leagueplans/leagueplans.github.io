package com.leagueplans.ui.projection.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.uicommon.utils.scala.DurationOps.safeAdd

import scala.concurrent.duration.{Duration, DurationLong}

/** When a step happens in the plan, and how long it takes. A duration is None when nothing in it
  * takes any time, and a start is None when nothing before it does either.
  *
  * @param start when the step first starts
  * @param durationPerRep how long its first repetition takes, substeps included
  * @param firstPass how long it takes, all of its repetitions included, the first time it's played.
  *                  A step inside another repeated step is played once for each of their
  *                  repetitions.
  * @param total how long it takes across every time it's played, which is how much time it adds to
  *              the plan
  * @param totalReps how many repetitions it plays across every time it's played
  * @param insideLoop whether a step it's part of repeats
  */
final case class StepTiming(
  start: Option[Duration],
  durationPerRep: Option[Duration],
  firstPass: Option[Duration],
  total: Option[Duration],
  totalReps: Int,
  insideLoop: Boolean
) {
  val finish: Option[Duration] =
    (start, firstPass) match {
      case (Some(s), Some(d)) => Some(s.safeAdd(d))
      case (s, None) => s
      case (None, d) => d
    }
}

object StepTiming {
  /** Before the plan's timings are known */
  val unknown: StepTiming =
    StepTiming(None, None, None, None, totalReps = 0, insideLoop = false)

  // Durations are sent between threads in nanoseconds, with the longest value standing in for
  // an infinite one
  given durationEncoder: Encoder[Duration] =
    Encoder.longEncoder.contramap(d => if (d.isFinite) d.toNanos else Long.MaxValue)

  given durationDecoder: Decoder[Duration] =
    Decoder.longDecoder.map(n => if (n == Long.MaxValue) Duration.Inf else n.nanos)

  given Encoder[StepTiming] = Encoder.derived
  given Decoder[StepTiming] = Decoder.derived
}
