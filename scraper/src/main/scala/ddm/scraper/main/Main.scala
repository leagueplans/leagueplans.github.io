package ddm.scraper.main

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.`User-Agent`
import ddm.scraper.dumper.{ItemDumper, SkillIconDumper}
import ddm.scraper.http.ThrottledHttpClient
import ddm.scraper.reporter.Reporter
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.scraper.{ItemScraper, SkillIconScraper}

import java.nio.file.{Files, Path}
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Main extends App {
  private val clArgs = CommandLineArgs.parse(args)
  private val targetDirectory = clArgs.get("target-directory")(Path.of(_))
  private val dumpDirectory = targetDirectory.resolve("dump")
  private val baseURL = "https://oldschool.runescape.wiki"

  private implicit val system: ActorSystem[Reporter.Message] =
    ActorSystem(
      Reporter.init(
        baseURL,
        data => Files.write(
          targetDirectory.resolve("report.md"),
          data.getBytes
        ): @nowarn("msg=discarded non-Unit value")
      ),
      "reporter"
    )

  import system.executionContext

  private val client = new MediaWikiClient(
    new ThrottledHttpClient(
      maxThroughput = 5,
      interval = 1.second,
      bufferSize = Int.MaxValue,
      parallelism = 4
    ),
    userAgent = clArgs.get("user-agent")(`User-Agent`(_)),
    baseURL
  )

  private val fCompletion =
    clArgs.get("scraper") {
      case "items" => scrapeItems()
      case "skill-icons" => scrapeSkillIcons()
    }

  fCompletion.onComplete(runStatus =>
    system ! Reporter.Message.Publish(runStatus)
  )

  private def scrapeItems(): Future[_] = {
    val source =
      clArgs
        .getOpt("pages")(pages => ItemScraper.scrape(pages.split('|').map(Page.Name.Other).toList, client, system))
        .orElse(clArgs.getOpt("from")(name => ItemScraper.scrapeFrom(Page.Name.Other(name), client, system)))
        .getOrElse(ItemScraper.scrapeAll(client, system))

    val idMapPath = clArgs.get("id-map")(Path.of(_))
    val itemDataTarget = dumpDirectory.resolve("data/items.json")
    val imagesRootTarget = dumpDirectory.resolve("images/items")
    Files.createDirectories(itemDataTarget.getParent)

    source.runWith(
      ItemDumper.dump(
        idMapPath,
        itemDataTarget,
        imagesRootTarget
      )
    )
  }

  private def scrapeSkillIcons(): Future[_] =
    SkillIconScraper
      .scrape(client, system)
      .runWith(SkillIconDumper.dump(dumpDirectory.resolve("images/skill-icons")))
}
