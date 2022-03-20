package ddm.scraper.wiki.scrapers

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import ddm.scraper.http.ThrottledHttpClient
import ddm.scraper.wiki.{WikiBrowser, WikiFetcher}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Using

trait Scraper {
  final def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "scraper")
    import actorSystem.executionContext

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
        browser => Await.result(run(browser, targetDirectory), 6.hours) // GitHub runner timeout
      }

    actorSystem.terminate()
    result.get
  }

  private def createBrowser(actorSystem: ActorSystem[_]): WikiBrowser[JsoupBrowser] =
    new WikiBrowser[JsoupBrowser](
      new JsoupBrowser(),
      new WikiFetcher(
        new ThrottledHttpClient(
          elements = 5,
          per = 1.second,
          bufferSize = Int.MaxValue,
          parallelism = 4
        )(actorSystem),
        maybeStore = None
      )(actorSystem.executionContext)
    )(_ => ())

  def run[B <: Browser](
    browser: WikiBrowser[B],
    targetDirectory: Path
  )(implicit mat: Materializer, ec: ExecutionContext): Future[Unit]
}
