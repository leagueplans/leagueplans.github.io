package ddm.scraper.core

import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.{Path, Paths}
import java.time.Clock
import java.util.logging.{Level, Logger}
import scala.util.Using

trait Scraper {
  def main(args: Array[String]): Unit = {
    Logger
      .getLogger("com.gargoylesoftware.htmlunit")
      .setLevel(Level.OFF)

    val targetDirectory = args match {
      case Array(directoryName) => Paths.get(directoryName)
      case _ => throw new IllegalArgumentException(
        "Unexpected or missing arguments. Expected usage: \n" +
          "  run <targetDirectoryName>\n" +
          "where <targetDirectoryName> is the relative location to create scraped files in"
      )
    }

    Using.resource(createFetcher())(run(_, targetDirectory))
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

  def run(pageFetcher: PageFetcher[HtmlUnitBrowser], targetDirectory: Path): Unit
}
