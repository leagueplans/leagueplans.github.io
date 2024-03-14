package ddm.scraper.wiki.scraper

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import ddm.scraper.dumper.Cache
import ddm.scraper.reporter.Reporter
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.model.Page.Name

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object SkillIconScraper {
  def scrape(
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]
  )(using ec: ExecutionContext): Source[(String, Array[Byte]), ?] =
    client
      .fetch(
        MediaWikiSelector.Members(Page.Name.Category("Skill icons")),
        maybeContent = None
      )
      .collect { case (page @ Page(_, name: Page.Name.File), _) if name.raw.contains("icon") => (page, name) }
      .mapAsync(parallelism = 10)((page, name) => fetchImage(name, client).map((page, _)))
      .via(Reporter.pageFlow(reporter))
      .map { case (_, (fileName, data)) =>
        val trimmed = fileName.raw.replaceFirst(" icon", "")
        s"$trimmed.${fileName.extension}" -> data
      }

  private def fetchImage(
    name: Page.Name.File,
    client: MediaWikiClient
  )(using ec: ExecutionContext): Future[Either[Throwable, (Name.File, Array[Byte])]] =
    client
      .fetchImage(name)
      .transform(result => Success(result.toEither.map((name, _))))
}
