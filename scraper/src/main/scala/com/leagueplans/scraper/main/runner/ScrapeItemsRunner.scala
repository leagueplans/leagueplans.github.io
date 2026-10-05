package com.leagueplans.scraper.main.runner

import com.leagueplans.common.model.{AcceptedItems, InfoboxKey, ItemData}
import com.leagueplans.scraper.dumper.items.ItemDumper
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.wiki.http.WikiClient
import com.leagueplans.scraper.wiki.scraper.ItemsScraper
import com.leagueplans.scraper.wiki.streaming.PageStream
import io.circe.parser.decode
import zio.{Chunk, RIO, Scope, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import scala.util.{Success, Try}

object ScrapeItemsRunner {
  def make(
    args: CommandLineArgs,
    targetDirectory: Path,
    client: WikiClient
  )(using Trace): Task[ScrapeItemsRunner] =
    for {
      originalItems <- ZIO.fromTry(loadOriginalData(args))
      scraper <- ZIO.fromTry(ItemsScraper.make(args, client, originalItems))
      dumper <- ItemDumper.make(originalItems, targetDirectory)
    } yield ScrapeItemsRunner(scraper, dumper)

  /** The accepted item data named by the `original-items` argument. None has been accepted
    * yet if the file doesn't exist.
    */
  private def loadOriginalData(args: CommandLineArgs): Try[Map[InfoboxKey, ItemData]] =
    args.get("original-items")(path => Try(Path.of(path))).flatMap(path =>
      Try(Files.exists(path)).flatMap {
        case true =>
          for {
            contents <- Try(Files.readString(path))
            accepted <- decode[AcceptedItems](contents).toTry
          } yield accepted.data.toMap

        case false => Success(Map.empty)
      }
    )
}

final class ScrapeItemsRunner(scraper: ItemsScraper, dumper: ItemDumper) extends ScrapeRunner {
  def run(using Trace): RIO[Scope, Chunk[PageStream.Error]] =
    for {
      (errorStream, itemStream) <- scraper.scrape.partitionEither(ZIO.succeed(_))
      fork <- errorStream.runCollect.forkScoped
      items <- itemStream.run(dumper.sink)
      errors <- fork.join
      _ <- dumper.dumpChangeset(items, errors)
    } yield errors
}
