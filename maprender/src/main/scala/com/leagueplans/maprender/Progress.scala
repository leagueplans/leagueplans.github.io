package com.leagueplans.maprender

import scala.concurrent.duration.{DurationLong, FiniteDuration}

object Progress {
  /** Like `4m05s`, or `45s` under a minute */
  def format(duration: FiniteDuration): String = {
    val seconds = duration.toSeconds
    if (seconds < 60) s"${seconds}s"
    else f"${seconds / 60}m${seconds % 60}%02ds"
  }
}

/** Counts the steps of a long, evenly paced piece of work, such as drawing every region, and
  * says how far it has got at most once per interval, so that a log shows it moving without
  * flooding.
  *
  * @param steps what a step is, in the plural, such as "regions drawn"
  * @param now the current time in nanoseconds
  */
final class Progress(
  total: Int,
  steps: String,
  interval: FiniteDuration,
  now: () => Long = () => System.nanoTime()
) {
  private val startedAt = now()
  private var done = 0
  private var reportedAt = startedAt

  /** Counts one step, and says how far the work has got if an interval has passed since it
    * last did */
  def step(): Option[String] = {
    done += 1
    val time = now()
    Option.when(time - reportedAt >= interval.toNanos) {
      reportedAt = time
      val remaining = ((time - startedAt) * (total - done) / done).nanos
      s"$done of $total $steps (${100 * done / total}%), about ${Progress.format(remaining)} left"
    }
  }

  def elapsed: FiniteDuration =
    (now() - startedAt).nanos
}
