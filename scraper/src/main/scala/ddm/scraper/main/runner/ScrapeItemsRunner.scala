package ddm.scraper.main.runner

import ddm.scraper.dumper.items.ItemDumper
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.WikiClient
import ddm.scraper.wiki.scraper.ItemsScraper
import ddm.scraper.wiki.streaming.PageStream
import zio.{Chunk, RIO, Scope, Task, Trace, ZIO}

import java.nio.file.Path

object ScrapeItemsRunner {
  def make(
    args: CommandLineArgs,
    targetDirectory: Path,
    client: WikiClient
  )(using Trace): Task[ScrapeItemsRunner] =
    for {
      scraper <- ZIO.fromTry(ItemsScraper.make(args, client))
      dumper <- ItemDumper.make(args, targetDirectory)
    } yield ScrapeItemsRunner(scraper, dumper)
}

final class ScrapeItemsRunner(scraper: ItemsScraper, dumper: ItemDumper) extends ScrapeRunner {
  def run(using Trace): RIO[Scope, Chunk[PageStream.Error]] =
    for {
      (errorStream, itemStream) <- scraper.scrape.partitionEither(ZIO.succeed(_))
      fork <- errorStream.runCollect.forkScoped
      _ <- itemStream.run(dumper.sink)
      errors <- fork.join
    } yield errors
}
