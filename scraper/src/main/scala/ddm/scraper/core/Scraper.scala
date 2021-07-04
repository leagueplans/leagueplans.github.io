package ddm.scraper.core

import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.Paths
import java.time.Clock
import java.util.logging.{Level, Logger}
import scala.util.Using

trait Scraper {
  def main(args: Array[String]): Unit = {
    Logger
      .getLogger("com.gargoylesoftware.htmlunit")
      .setLevel(Level.OFF)

    Using.resource(createFetcher())(run)
  }

  private def createFetcher(): PageFetcher[HtmlUnitBrowser] = {
    new PageFetcher[HtmlUnitBrowser](
      new HtmlUnitBrowser(),
      HtmlLogger.prepare(
        logDirectory = Paths.get("logs/html"),
        Clock.systemUTC()
      )
    )(_.closeAll())
  }

  def run(pageFetcher: PageFetcher[HtmlUnitBrowser]): Unit
}
