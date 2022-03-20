package ddm.scraper.wiki

import akka.http.scaladsl.model.HttpRequest
import ddm.scraper.http.ThrottledHttpClient

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

object WikiFetcher {
  private val baseUrl: String = "https://oldschool.runescape.wiki"
}

final class WikiFetcher(client: ThrottledHttpClient, maybeStore: Option[FileStore])(
  implicit ec: ExecutionContext
) {
  def fetch(wikiPath: String): Array[Byte] =
    Future(recover(wikiPath))
      .flatMap {
        case Some(data) => Future.successful(data)
        case None =>
          val fData = client.queue(HttpRequest(uri = resolve(wikiPath)))
          fData.foreach(data => maybeStore.map(_.persist(wikiPath, data)))
          fData
      }
      .pipe(Await.result(_, 1.minute)) // It's a hobby project. I don't want to bother with Futures

  private def recover(wikiPath: String): Option[Array[Byte]] =
    for {
      store <- maybeStore
      data <- store.recover(wikiPath)
    } yield data

  private def resolve(wikiPath: String): String =
    s"${WikiFetcher.baseUrl}$wikiPath"
}
