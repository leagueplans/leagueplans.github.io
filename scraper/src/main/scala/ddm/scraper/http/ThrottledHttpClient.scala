package ddm.scraper.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult}
import org.log4s.{Logger, getLogger}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

final class ThrottledHttpClient(
  maxThroughput: Int,
  interval: FiniteDuration,
  bufferSize: Int,
  parallelism: Int
)(implicit actorSystem: ActorSystem[_]) {
  private val http = Http()
  import actorSystem.executionContext

  private val logger: Logger = getLogger

  private val requestExecutor =
    Source
      .queue[(HttpRequest, Promise[HttpResponse])](
        bufferSize,
        overflowStrategy = OverflowStrategy.dropNew
      )
      .throttle(maxThroughput, interval)
      .zipWithIndex
      .mapAsync(parallelism) { case ((request, pResponse), requestId) =>
        logger.info(s"Executing [$requestId]: [${request.method.value} ${request.uri}]")
        pResponse
          .completeWith(http.singleRequest(request))
          .future
          .transform { maybeResponse =>
            maybeResponse match {
              case Success(response) if response.status.isSuccess() =>
                logger.debug(s"Response [$requestId]: [$response]")
              case Success(response) =>
                logger.warn(s"Response [$requestId]: [$response]")
              case Failure(error) =>
                logger.error(error)(s"Request [$requestId] failed")
            }
            Success(())
          }
      }
      .toMat(Sink.ignore)(Keep.left)
      .run()

  def queue(request: HttpRequest): Future[(StatusCode, Array[Byte])] = {
    val pResponse = Promise[HttpResponse]()
    for {
      _ <- requestExecutor
             .offer((request, pResponse))
             .map {
               case QueueOfferResult.Enqueued => pResponse
               case QueueOfferResult.Dropped => pResponse.failure(new RuntimeException("WebClient buffer full"))
               case QueueOfferResult.QueueClosed => pResponse.failure(new RuntimeException("WebClient stream closed"))
               case QueueOfferResult.Failure(t) => pResponse.failure(t)
             }
      response <- pResponse.future
      bytes    <- Unmarshal(response).to[Array[Byte]]
    } yield (response.status, bytes)
  }
}
