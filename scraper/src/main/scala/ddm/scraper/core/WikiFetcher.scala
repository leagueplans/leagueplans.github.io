package ddm.scraper.core

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.HttpRequest
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import net.ruippeixotog.scalascraper.browser.Browser
import org.log4s.MDC

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Using.Releasable
import scala.util.chaining.scalaUtilChainingOps

object WikiFetcher {
  private val baseUrl: String = "https://oldschool.runescape.wiki"
}

final class WikiFetcher[B <: Browser](
  browser: B,
  http: HttpExt,
  pageLogger: PageLogger
)(implicit releasable: Releasable[B], materializer: Materializer) extends AutoCloseable {
  def fetchHtml(wikiPath: String): B#DocumentType =
    MDC.withCtx("wiki-path" -> wikiPath) {
      pageLogger.recoverHtml(wikiPath) match {
        case Some(rawHtml) =>
          browser.parseString(rawHtml)
        case None =>
          val doc = browser.get(resolve(wikiPath))
          pageLogger.logHtml(wikiPath, doc.toHtml)
          doc
      }
    }

  def fetchFile(wikiPath: String): Array[Byte] =
    Source
      .single(HttpRequest(uri = resolve(wikiPath)))
      .mapAsync(parallelism = 1)(http.singleRequest(_))
      .flatMapConcat(_.entity.dataBytes)
      .runWith(Sink.fold(ByteString.empty)(_ ++ _))
      .pipe(Await.result(_, 4.seconds))
      .toArray

  private def resolve(wikiPath: String): String =
    s"${WikiFetcher.baseUrl}$wikiPath"

  def close(): Unit =
    releasable.release(browser)
}
