package ddm.scraper.core

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.chaining.scalaUtilChainingOps

object WikiFetcher {
  private val baseUrl: String = "https://oldschool.runescape.wiki"
}

final class WikiFetcher(client: ThrottledWebClient, store: FileStore)(
  implicit ec: ExecutionContext
) {
  def fetch(wikiPath: String): Array[Byte] =
    Future(store.recover(wikiPath))
      .flatMap {
        case Some(data) => Future.successful(data)
        case None =>
          val fData = client.queue(HttpRequest(uri = resolve(wikiPath)))
          fData.foreach(store.persist(wikiPath, _))
          fData
      }
      .pipe(Await.result(_, 1.minute)) // It's a hobby project. I don't want to bother with Futures

  private def resolve(wikiPath: String): String =
    s"${WikiFetcher.baseUrl}$wikiPath"
}
