package ddm.scraper.ratelimiting

import ddm.scraper.telemetry.Metric
import zio.{Duration, Exit, IO, Promise, Queue, Ref, Schedule, Scope, Trace, UIO, URIO, ZIO}

import scala.concurrent.duration.FiniteDuration

object TokenBucketRateLimiter {
  export Builder.make

  /** A signal that indicates when a request may be executed */
  private type Signal = Promise[Nothing, Unit]

  extension (self: Promise[?, ?]) {
    private def hasTriggered(using Trace): UIO[Boolean] =
      self.isDone.exit.map {
        case Exit.Success(isDone) => isDone
        case Exit.Failure(_) => true
      }
  }

  private object Bucket {
    def make(name: String, capacity: Int)(using Trace): UIO[Bucket] =
      for {
        tokenCount <- Ref.Synchronized.make(capacity)
        gauge <- Metric.makeGaugeT[Int](s"$name.rate-limiting.token-bucket.available-tokens")(_.toDouble)
        _ <- gauge.set(capacity)
      } yield Bucket(capacity, tokenCount, gauge)
  }

  private final class Bucket(
    capacity: Int,
    tokenCount: Ref.Synchronized[Int],
    gauge: Metric.Gauge[Int]
  ) {
    def getTokenCount(using Trace): UIO[Int] =
      tokenCount.get

    def withdrawToken(using Trace): UIO[Boolean] =
      tokenCount.getAndUpdateSomeZIO { case current if current > 0 =>
        val next = current - 1
        gauge.set(next).as(next)
      }.map(_ > 0)

    def depositToken(using Trace): UIO[Unit] =
      tokenCount.updateSomeZIO { case current if current < capacity =>
        val next = current + 1
        gauge.set(next).as(next)
      }
  }

  private object Builder {
    /** @param maxBurstSize the maximum number of tokens that can be held for future requests
      * @param interval     the desired duration between each tick of the rate limiter
      */
    def make(
      name: String,
      maxBurstSize: Int,
      interval: FiniteDuration
    )(using Trace): URIO[Scope, TokenBucketRateLimiter] =
      for {
        buffer <- ZIO.acquireRelease(Queue.unbounded[Signal])(_.shutdown)
        bucket <- Bucket.make(name, maxBurstSize)
        schedule = Schedule.fixed(Duration.fromScala(interval))
        _ <- onTick(buffer, bucket).schedule(schedule).forkScoped
        bufferDepthGauge <- Metric.makeGaugeT[Int](s"$name.rate-limiting.token-bucket.buffer-depth")(_.toDouble)
      } yield TokenBucketRateLimiter(buffer, bucket, bufferDepthGauge)

    private def onTick(buffer: Queue[Signal], bucket: Bucket)(using Trace): UIO[?] =
      buffer.poll.flatMap {
        case None =>
          bucket.depositToken
        case Some(signal) =>
          ZIO.ifZIO(signal.hasTriggered)(
            onTrue = onTick(buffer, bucket),
            onFalse = signal.succeed(())
          )
      }
  }
}

/** A rate limiter, implemented according to the token bucket algorithm.
  *
  * https://en.wikipedia.org/wiki/Token_bucket
  */
final class TokenBucketRateLimiter private (
  buffer: Queue[TokenBucketRateLimiter.Signal],
  bucket: TokenBucketRateLimiter.Bucket,
  bufferDepthGauge: Metric.Gauge[Int]
) extends RateLimiter {
  def await(using Trace): IO[RateLimiter.Error, Unit] =
    bucket.withdrawToken.flatMap {
      case true => ZIO.unit
      case false => raceShutdown(Promise.make[Nothing, Unit].flatMap(waitForSignal))
    }

  private def raceShutdown[R, A](f: ZIO[R, RateLimiter.Error, A])(using Trace): ZIO[R, RateLimiter.Error, A] =
    awaitShutdown.disconnect.raceFirst(f.disconnect)

  private def awaitShutdown(using Trace): IO[RateLimiter.Error, Nothing] =
    buffer.awaitShutdown.exit.as(RateLimiter.Error.Shutdown).flip

  private def waitForSignal(signal: TokenBucketRateLimiter.Signal)(using Trace): IO[RateLimiter.Error, Unit] =
    ZIO.uninterruptibleMask { restore =>
      // Termination could be because we've shutdown, but it could also be because the requester is
      // no longer interested in the buffered request and has stopped waiting. Completing the signal
      // here allows us to skip the request when its turn in the queue arrives.
      val waiting = restore(signal.await).onTermination(signal.failCause)

      ZIO.ifZIO(buffer.offer(signal))(
        onTrue = bufferDepthGauge.increment *> waiting.onExit(_ => bufferDepthGauge.decrement),
        onFalse = restore(ZIO.fail(RateLimiter.Error.FailedToBuffer))
      )
    }
}
