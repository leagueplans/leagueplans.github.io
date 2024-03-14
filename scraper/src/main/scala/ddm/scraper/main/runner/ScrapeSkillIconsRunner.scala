package ddm.scraper.main.runner

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import ddm.scraper.dumper.{Cache, SkillIconDumper}
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.scraper.SkillIconScraper

import java.nio.file.Path
import scala.concurrent.ExecutionContext

object ScrapeSkillIconsRunner {
  def from(args: CommandLineArgs): ScrapeSkillIconsRunner =
    ScrapeSkillIconsRunner(
      args.get("target-directory")(root =>
        Path.of(root).resolve("dump/dynamic/assets/images/skill-icons")
      )
    )
}

final class ScrapeSkillIconsRunner(iconsDirectory: Path) extends Runner {
  def run(
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message[(Page, Throwable)]],
  )(using mat: Materializer, ec: ExecutionContext): Unit =
    SkillIconScraper
      .scrape(client, reporter)
      .runWith(SkillIconDumper.dump(iconsDirectory))
      .onComplete(runStatus => reporter ! Cache.Message.Complete(runStatus))
}
