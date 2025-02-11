package com.leagueplans.scraper.http

import com.leagueplans.scraper.ratelimiting.{RateLimitException, RateLimiter, TokenBucketRateLimiter}
import com.leagueplans.scraper.telemetry.{MeasuredSemaphore, Metric, WithAnnotation}
import zio.http.*
import zio.http.netty.NettyConfig
import zio.{Duration, IO, RIO, Schedule, Scope, Task, Trace, UIO, ZIO, ZLayer}

import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object HTTPClient {
  export Builder.make

  private object Builder {
    def make(
      name: String,
      connectionPoolSize: Int,
      idleTimeout: FiniteDuration,
      maxRequestBurst: Int,
      rateLimitInterval: FiniteDuration
    )(using Trace): RIO[Scope, HTTPClient] = {
      val resources =
        ZIO.acquireReleaseInterruptible(
          makeBaseClient(connectionPoolSize, idleTimeout) <&>
            TokenBucketRateLimiter.make(s"$name.http-client", maxRequestBurst, rateLimitInterval)
        )(withClientAnnotation(name)(ZIO.logInfo("Shutting down HTTP client")))

      for {
        (shutdown, (baseClient, rateLimiter)) <- resources.withEarlyRelease
        connectionPermits <- MeasuredSemaphore.make(s"$name.http-client.connection-pool", connectionPoolSize)
        requestCounter <- Metric.makeCounter(s"$name.http-client.requests")
        responseCounter <- Metric.makeCounter(s"$name.http-client.responses")
      } yield HTTPClient(name, baseClient, shutdown, rateLimiter, connectionPermits, requestCounter, responseCounter)
    }

    private def makeBaseClient(
      connectionPoolSize: Int,
      idleTimeout: FiniteDuration
    )(using Trace): RIO[Scope, Client] = {
      val dependencies =
        ZLayer.succeed(config(connectionPoolSize, idleTimeout)) ++
          ZLayer.succeed(NettyConfig.default) ++
          DnsResolver.default

      dependencies.to(ZClient.live.fresh).build.map(_.get)
    }

    private def config(connectionPoolSize: Int, idleTimeout: FiniteDuration): ZClient.Config =
      ZClient.Config.default
        .dynamicConnectionPool(
          minimum = 0,
          maximum = connectionPoolSize,
          ttl = Duration.fromScala(idleTimeout)
        )
        .idleTimeout(Duration.fromScala(idleTimeout))
        .connectionTimeout(Duration.fromScala(idleTimeout))
        .addUserAgentHeader(false)
  }

  private def withClientAnnotation(name: String): WithAnnotation =
    WithAnnotation.forLogs("http-client" -> name)
}

final class HTTPClient(
  name: String,
  underlying: Client,
  shutdown: UIO[Unit],
  rateLimiter: RateLimiter,
  connectionPermits: MeasuredSemaphore,
  requestCounter: Metric.Counter[Long],
  responseCounter: Metric.Counter[Long],
) {
  def execute(
    request: Request,
    kindLabel: String,
    retryPolicy: Schedule[Any, Response, ?]
  )(using Trace): Task[Response] = {
    val withRequestAnnotations =
      WithAnnotation.forLogs(
        "request-id" -> UUID.randomUUID().toString,
        "request-kind" -> kindLabel,
        "request" -> s"${request.method.name} ${request.url.encode}",
      ) @@ HTTPClient.withClientAnnotation(name)

    val withKindMetricLabel = WithAnnotation.forMetrics("request-kind" -> kindLabel)
    val schedule = addResponseLogging(retryPolicy, recordResponse(_, withKindMetricLabel))

    val iteration =
      withKindMetricLabel(requestCounter.increment) *>
        ZIO.logTrace("Request queued") *>
        defer(request)

    withRequestAnnotations(iteration.repeat(schedule))
  }

  private def defer(request: Request)(using Trace): Task[Response] =
    connectionPermits.withPermit(
      rateLimit *>
        ZIO.logDebug("Executing request") *>
        underlying.batched(request)
    )

  private def rateLimit(using Trace): IO[RateLimitException, Unit] =
    rateLimiter.await.tapSomeError { case RateLimiter.Error.Shutdown =>
      ZIO.logWarning(
        "Failed request because the rate limiter has already shutdown. Triggering client shutdown..."
      ) *> shutdown.forkDaemon
    }.mapError(_.exception())

  private def recordResponse(
    response: Response,
    withKindMetricLabel: WithAnnotation
  )(using Trace): UIO[Unit] =
    WithAnnotation("status" -> response.status.text)(
      withKindMetricLabel(responseCounter.increment) *>
        ZIO.logDebug("Received response")
    )

  private def addResponseLogging(
    retryPolicy: Schedule[Any, Response, ?],
    recordResponse: Response => UIO[Unit]
  )(using Trace): Schedule[Any, Response, Response] =
    (Schedule.count && Schedule.identity[Response] <* retryPolicy)
      .onDecision { case (_, (retryCount, response), _) =>
        WithAnnotation.forLogs("retry-count" -> retryCount.toString)(recordResponse(response))
      }
      .onDecision {
        case (_, (retryCount, response), Schedule.Decision.Done) =>
          ZIO.unit
        case (_, (retryCount, response), _: Schedule.Decision.Continue) =>
          WithAnnotation.forLogs("retry-count" -> (retryCount + 1).toString)(
            ZIO.logDebug("Scheduling retry for request")
          )
      }
      .map((_, response) => response)
}
