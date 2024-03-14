package ddm.scraper.wiki.scraper.leagues

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import ddm.common.model.LeagueTask
import ddm.scraper.wiki.decoder.leagues.{TrailblazerReloadedTaskDecoder, TrailblazerReloadedTaskRowExtractor}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.parser.TermParser

object TrailblazerReloadedTasksScraper {
  def scrape(
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[LeagueTask, ?] = {
    val taskRowExtractor = new TrailblazerReloadedTaskRowExtractor

    client
      .fetch(
        MediaWikiSelector.Pages(List(Page.Name.from("Trailblazer_Reloaded_League/Tasks"))),
        Some(MediaWikiContent.Revisions)
      )
      .via(errorReportingFlow(reportError))
      .map((page, content) => (page, TermParser.parse(content)))
      .via(errorReportingFlow(reportError))
      .map((page, terms) => (page, taskRowExtractor.extract(terms)))
      .via(errorReportingFlow(reportError))
      .mapConcat((page, tasks) =>
        tasks.map((index, task) =>
          (page, TrailblazerReloadedTaskDecoder.decode(index, task))
        )
      )
      .via(errorReportingFlow(reportError))
      .map((_, task) => task)
  }

  private def errorReportingFlow[T](
    reportError: (Page, Throwable) => Unit
  ): Flow[(Page, Either[Throwable, T]), (Page, T), NotUsed] =
    Flow[(Page, Either[Throwable, T])]
      .collect(Function.unlift {
        case (page, Right(value)) =>
          Some((page, value))
        case (page, Left(error)) =>
          reportError(page, error)
          None
      })
}
