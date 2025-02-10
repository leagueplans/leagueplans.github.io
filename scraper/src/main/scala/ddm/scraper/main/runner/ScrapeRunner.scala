package ddm.scraper.main.runner

import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.WikiClient
import ddm.scraper.wiki.streaming.PageStream
import zio.{Chunk, RIO, Scope, Task, Trace, ZIO}

import java.nio.file.Path
import scala.util.{Failure, Success}

object ScrapeRunner {
  def make(
    args: CommandLineArgs,
    targetDirectory: Path,
    client: WikiClient
  ): Task[ScrapeRunner] =
    ZIO.fromTry(args.get("scraper") {
      case "items" => Success(ScrapeItemsRunner.make(args, targetDirectory, client))
      case "league-tasks" => Success(ScrapeLeagueTasksRunner.make(args, targetDirectory, client))
      case "skill-icons" => Success(ScrapeSkillIconsRunner.make(targetDirectory, client))
      case other => Failure(IllegalArgumentException(s"Unexpected scraper key [$other]"))
    }).flatten
}

trait ScrapeRunner {
  def run(using Trace): RIO[Scope, Chunk[PageStream.Error]]
}
