package ddm.scraper.wiki.scraper.leagues

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import ddm.common.model.LeagueTask
import ddm.scraper.wiki.decoder.leagues.{LeagueTaskRowExtractor, TwistedTaskDecoder}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.parser.TermParser

object TwistedTasksScraper {
  def scrape(
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[LeagueTask, _] = {
    val taskRowExtractor = new LeagueTaskRowExtractor

    client
      .fetch(
        MediaWikiSelector.Pages(List(Page.Name.from("Twisted_League/Tasks"))),
        Some(MediaWikiContent.Revisions)
      )
      .via(errorReportingFlow(reportError))
      .map { case (page, content) => (page, TermParser.parse(content)) }
      .via(errorReportingFlow(reportError))
      .map { case (page, terms) => (page, taskRowExtractor.extract(terms)) }
      .via(errorReportingFlow(reportError))
      .mapConcat { case (page, sections) =>
        sections.flatMap(section =>
          section.tasks.map { case (index, task) =>
            (page, TwistedTaskDecoder.decode(index, section.tier, task))
          }
        )
      }
      .via(errorReportingFlow(reportError))
      .map { case (_, task) => task }
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
