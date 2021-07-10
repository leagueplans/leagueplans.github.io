package ddm.scraper.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.stream.Materializer
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.{Files, Path, Paths}
import java.util.logging.{Level, Logger}
import scala.util.Using

trait Scraper {
  def main(args: Array[String]): Unit = {
    Logger
      .getLogger("com.gargoylesoftware.htmlunit")
      .setLevel(Level.OFF)

    val actorSystem = ActorSystem(Behaviors.empty, "scraper")
    val materializer = Materializer(actorSystem)

    val targetDirectory = args match {
      case Array(directoryName) => Paths.get(directoryName)
      case _ => throw new IllegalArgumentException(
        "Unexpected or missing arguments. Expected usage: \n" +
          "  run <targetDirectoryName>\n" +
          "where <targetDirectoryName> is the relative location to create scraped files in"
      )
    }

    val result =
      Using(createFetcher(materializer)) {
        Files.createDirectories(targetDirectory)
        run(_, targetDirectory)
      }

    actorSystem.terminate()
    result.get
  }

  private def createFetcher(materializer: Materializer): WikiFetcher[HtmlUnitBrowser] = {
    new WikiFetcher[HtmlUnitBrowser](
      new HtmlUnitBrowser(),
      Http()(materializer.system),
      PageLogger.prepare(logDirectory = Paths.get("logs/html"))
    )(_.closeAll(), materializer)
  }

  def run(
    pageFetcher: WikiFetcher[HtmlUnitBrowser],
    targetDirectory: Path
  ): Unit
}
