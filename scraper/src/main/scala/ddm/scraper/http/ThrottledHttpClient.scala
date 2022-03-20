package ddm.scraper.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OverflowStrategy
import akka.stream.QueueOfferResult._
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.log4s.{Logger, getLogger}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Success

final class ThrottledHttpClient(
  elements: Int,
  per: FiniteDuration,
  parallelism: Int
)(implicit actorSystem: ActorSystem[_]) {
  private val http = Http()
  import actorSystem.executionContext

  private val logger: Logger = getLogger

  private val requestExecutor =
    Source
      .queue[(HttpRequest, Promise[HttpResponse])](
        bufferSize = Int.MaxValue,
        overflowStrategy = OverflowStrategy.dropNew
      )
      .throttle(elements, per)
      .zipWithIndex
      .mapAsync(parallelism) { case ((request, pResponse), requestId) =>
        logger.debug(s"Executing [$requestId]: [$request]")
        pResponse
          .completeWith(http.singleRequest(request))
          .future
          .transform { response =>
            logger.debug(s"Response [$requestId]: [$response]")
            Success(())
          }
      }
      .toMat(Sink.ignore)(Keep.left)
      .run()

  def queue(request: HttpRequest): Future[Array[Byte]] = {
    val pResponse = Promise[HttpResponse]()
    for {
      _ <- requestExecutor
             .offer((request, pResponse))
             .map {
               case Enqueued => pResponse
               case Dropped => pResponse.failure(new RuntimeException("WebClient buffer full"))
               case QueueClosed => pResponse.failure(new RuntimeException("WebClient stream closed"))
               case Failure(t) => pResponse.failure(t)
             }
      response <- pResponse.future
      bytes    <- Unmarshal(response).to[Array[Byte]]
    } yield bytes
  }
}
