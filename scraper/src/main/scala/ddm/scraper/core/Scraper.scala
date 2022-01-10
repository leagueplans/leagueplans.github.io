package ddm.scraper.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.{Files, Path, Paths}
import java.util.logging.{Level, Logger}
import scala.concurrent.duration.DurationInt
import scala.util.Using

trait Scraper {
  final def main(args: Array[String]): Unit = {
    Logger
      .getLogger("com.gargoylesoftware.htmlunit")
      .setLevel(Level.OFF)

    val actorSystem = ActorSystem(Behaviors.empty, "scraper")

    val targetDirectory = args match {
      case Array(directoryName) => Paths.get(directoryName)
      case _ => throw new IllegalArgumentException(
        "Unexpected or missing arguments. Expected usage: \n" +
          "  run <targetDirectoryName>\n" +
          "where <targetDirectoryName> is the relative location to create scraped files in"
      )
    }

    val result =
      Using(createBrowser(actorSystem)) {
        Files.createDirectories(targetDirectory)
        run(_, targetDirectory)
      }

    actorSystem.terminate()
    result.get
  }

  private def createBrowser(actorSystem: ActorSystem[_]): WikiBrowser[HtmlUnitBrowser] =
    new WikiBrowser[HtmlUnitBrowser](
      new HtmlUnitBrowser(),
      new WikiFetcher(
        new ThrottledWebClient(elements = 5, per = 1.second)(actorSystem),
        FileStore.prepare(directory = Paths.get("logs/data"))
      )(actorSystem.executionContext)
    )(_.closeAll())

  def run(
    browser: WikiBrowser[HtmlUnitBrowser],
    targetDirectory: Path
  ): Unit
}
