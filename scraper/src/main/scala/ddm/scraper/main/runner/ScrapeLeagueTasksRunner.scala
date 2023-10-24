package ddm.scraper.main.runner

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{Sink, Source}
import ddm.common.model.LeagueTask
import ddm.scraper.dumper.{Cache, CachingWriter, dataSink}
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.scraper.leagues.{ShatteredRelicsTasksScraper, TrailblazerTasksScraper, TwistedTasksScraper}

import java.nio.file.{Path, StandardOpenOption}
import scala.annotation.nowarn

object ScrapeLeagueTasksRunner {
  def from(args: CommandLineArgs): ScrapeLeagueTasksRunner =
    new ScrapeLeagueTasksRunner(
      args.get("league")(_.toInt),
      args.get("target-directory")(Path.of(_).resolve("dump/data/tasks.json"))
    )
}

final class ScrapeLeagueTasksRunner(
  league: Int,
  tasksFile: Path
) extends Runner {
  def run(
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message[(Page, Throwable)]],
    spawnWriter: Spawn[Cache.Message[LeagueTask]]
  )(implicit system: ActorSystem[_]): Unit = {
    val source =
      league match {
        case 1 =>
          TwistedTasksScraper.scrape(client, (page, error) => reporter ! Cache.Message.NewEntry((page, error)))
        case 2 =>
          TrailblazerTasksScraper.scrape(client, (page, error) => reporter ! Cache.Message.NewEntry((page, error)))
        case 3 =>
          ShatteredRelicsTasksScraper.scrape(client, (page, error) => reporter ! Cache.Message.NewEntry((page, error)))
        case _ =>
          Source.empty[LeagueTask]
      }

    val writerBehaviour = CachingWriter.to[LeagueTask](tasksFile, StandardOpenOption.CREATE_NEW)
    source
      .alsoTo(dataSink(spawnWriter(writerBehaviour)))
      .runWith(Sink.onComplete(runStatus => reporter ! Cache.Message.Complete(runStatus))): @nowarn("msg=discarded non-Unit value")
  }
}
