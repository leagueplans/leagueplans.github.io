package ddm.scraper.core

import net.ruippeixotog.scalascraper.browser.Browser

import scala.util.Using.Releasable

final class WikiBrowser[B <: Browser](browser: B, fetcher: WikiFetcher)(
  implicit releasable: Releasable[B]
) extends AutoCloseable {
  def fetchHtml(wikiPath: String): B#DocumentType =
    browser.parseString(
      new String(
        fetcher.fetch(wikiPath)
      )
    )

  def fetchFile(wikiPath: String): Array[Byte] =
    fetcher.fetch(wikiPath)

  def close(): Unit =
    releasable.release(browser)
}
