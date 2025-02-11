package com.leagueplans.scraper.main.runner

import com.leagueplans.scraper.dumper.items.ItemDumper
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.wiki.http.WikiClient
import com.leagueplans.scraper.wiki.scraper.ItemsScraper
import com.leagueplans.scraper.wiki.streaming.PageStream
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
