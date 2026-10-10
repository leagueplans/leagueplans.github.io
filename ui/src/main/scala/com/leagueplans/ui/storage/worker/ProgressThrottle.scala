package com.leagueplans.ui.storage.worker

/** Decides which progress reports are worth sending. Reading a plan can finish thousands of steps
  * a second, which is far more reports than anyone can see, so only the first report and those at
  * least `minIntervalMillis` after the last one sent are allowed. */
private final class ProgressThrottle(minIntervalMillis: Double, now: () => Double) {
  private var lastSent: Option[Double] = None

  def allow(): Boolean = {
    val time = now()
    val allowed = lastSent.forall(time - _ >= minIntervalMillis)
    if (allowed) lastSent = Some(time)
    allowed
  }
}
