package ddm.scraper.telemetry

import zio.stm.TSemaphore
import zio.{Trace, UIO, ZIO}

object MeasuredSemaphore {
  def make(name: String, permits: Long): UIO[MeasuredSemaphore] =
    for {
      semaphore <- TSemaphore.make(permits).commit
      availablePermits <- Metric.makeGaugeT[Long](s"$name.semaphore.available-permits")(_.toDouble)
      awaitingPermits <- Metric.makeGaugeT[Long](s"$name.semaphore.awaiting-permits")(_.toDouble)
      _ <- availablePermits.set(permits)
    } yield MeasuredSemaphore(semaphore, availablePermits, awaitingPermits)
}

final class MeasuredSemaphore(
  underlying: TSemaphore,
  availablePermits: Metric.Gauge[Long],
  awaitingPermits: Metric.Gauge[Long]
) {
  def withPermit[R, E, A](f: ZIO[R, E, A])(using Trace): ZIO[R, E, A] =
    ZIO.uninterruptibleMask { restore =>
      val acquirePermit =
        awaitingPermits.increment *>
          restore(underlying.acquire.commit).onExit(_ => awaitingPermits.decrement) *>
          availablePermits.decrement

      val run =
        restore(f)
          .onExit(_ => availablePermits.increment)
          .onExit(_ => underlying.release.commit)

      acquirePermit *> run
    }
}
