package com.leagueplans.scraper.main.runner

import com.leagueplans.scraper.dumper.ImageDumper
import com.leagueplans.scraper.wiki.http.WikiClient
import com.leagueplans.scraper.wiki.scraper.SkillIconScraper
import com.leagueplans.scraper.wiki.streaming.{PageStream, pageMapZIO, pageRun}
import zio.{Chunk, Task, Trace, UIO, ZIO}

import java.nio.file.Path

object ScrapeSkillIconsRunner {
  def make(targetDirectory: Path, client: WikiClient)(using Trace): Task[ScrapeSkillIconsRunner] =
    for {
      iconsDirectory <- ZIO.attempt(targetDirectory.resolve("dump/dynamic/assets/images/skill-icons"))
      dumper <- ImageDumper.make("skill-icons", iconsDirectory)
    } yield ScrapeSkillIconsRunner(dumper, client)
}

final class ScrapeSkillIconsRunner(dumper: ImageDumper, client: WikiClient) extends ScrapeRunner {
  def run(using Trace): UIO[Chunk[PageStream.Error]] =
    SkillIconScraper
      .scrape(client)
      .pageMapZIO((path, icon) => dumper.dump(path, icon))
      .pageRun
      .map((errors, _) => errors)
}
