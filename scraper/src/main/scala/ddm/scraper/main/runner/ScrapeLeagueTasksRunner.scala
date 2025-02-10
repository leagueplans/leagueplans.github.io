package ddm.scraper.main.runner

import ddm.common.model.LeagueTask
import ddm.scraper.dumper.JsonDumper
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.WikiClient
import ddm.scraper.wiki.scraper.leagues.*
import ddm.scraper.wiki.streaming.{PageStream, pageRun}
import zio.{Chunk, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import scala.util.{Failure, Success, Try}

object ScrapeLeagueTasksRunner {
  def make(
    args: CommandLineArgs,
    targetDirectory: Path,
    client: WikiClient
  )(using Trace): Task[ScrapeLeagueTasksRunner] =
    for {
      scraper <- makeScraper(args, client)
      dumper <- makeDumper(targetDirectory)
    } yield ScrapeLeagueTasksRunner(scraper, dumper)

  private def makeScraper(args: CommandLineArgs, client: WikiClient)(using Trace): Task[LeagueTasksScraper] =
    ZIO.fromTry(args.get("league") {
      case "1" => Success(TwistedTasksScraper.make(client))
      case "2" => Success(TrailblazerTasksScraper.make(client))
      case "3" => Success(ShatteredRelicsTasksScraper.make(client))
      case "4" => Success(TrailblazerReloadedTasksScraper.make(client))
      case "5" => Success(RagingEchoesTasksScraper.make(client))
      case s => Failure(IllegalArgumentException(s"Unexpected league number [$s]"))
    }).flatten

  private def makeDumper(targetDirectory: Path): Task[JsonDumper[Vector[LeagueTask]]] =
    ZIO.fromTry(
      for {
        targetFile <- Try(targetDirectory.resolve("dump/data/tasks.json"))
        _ <- Try(Files.createDirectories(targetFile.getParent))
        dumper <- JsonDumper.make[Vector[LeagueTask]](targetFile)
      } yield dumper
    )
}

final class ScrapeLeagueTasksRunner(
  scraper: LeagueTasksScraper,
  dumper: JsonDumper[Vector[LeagueTask]]
) extends ScrapeRunner {
  def run(using Trace): Task[Chunk[PageStream.Error]] =
    for {
      (errors, tasks) <- scraper.scrape.pageRun
      _ <- ZIO.fromTry(dumper.dump(
        tasks.map((_, task) => task).toVector.sorted
      ))
    } yield errors
}
