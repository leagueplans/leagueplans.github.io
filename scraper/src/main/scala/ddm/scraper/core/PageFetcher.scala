package ddm.scraper.core

import net.ruippeixotog.scalascraper.browser.Browser
import org.log4s.MDC

import scala.util.Using.Releasable

object PageFetcher {
  private val baseUrl: String = "https://oldschool.runescape.wiki/w/"
}

final class PageFetcher[B <: Browser](
  browser: B,
  htmlLogger: HtmlLogger
)(implicit releasable: Releasable[B]) extends AutoCloseable {
  def fetch(wikiPath: String): B#DocumentType =
    MDC.withCtx("wiki-path" -> wikiPath) {
      val doc = browser.get(s"${PageFetcher.baseUrl}$wikiPath")
      htmlLogger.log(doc)
      doc
    }

  def close(): Unit =
    releasable.release(browser)
}
