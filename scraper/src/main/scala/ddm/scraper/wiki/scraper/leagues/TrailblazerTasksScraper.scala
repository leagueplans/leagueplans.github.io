package ddm.scraper.wiki.scraper.leagues

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import ddm.common.model.{LeagueTask, LeagueTaskArea}
import ddm.scraper.wiki.decoder.leagues.{LeagueTaskRowExtractor, TrailblazerTaskDecoder}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.parser.TermParser

object TrailblazerTasksScraper {
  def scrape(
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[LeagueTask, ?] = {
    val taskRowExtractor = new LeagueTaskRowExtractor

    Source(pagesWithAreas)
      .flatMapConcat((area, pageName) =>
        client
          .fetch(MediaWikiSelector.Pages(List(pageName)), Some(MediaWikiContent.Revisions))
          .map((page, result) => (page, result.map(content => (area, content))))
      )
      .via(errorReportingFlow(reportError))
      .map { case (page, (area, content)) =>
        (page, TermParser.parse(content).map(terms => (area, terms)))
      }
      .via(errorReportingFlow(reportError))
      .map { case (page, (area, terms)) =>
        (page, taskRowExtractor.extract(terms).map(sections => (area, sections)))
      }
      .via(errorReportingFlow(reportError))
      .mapConcat { case (page, (area, sections)) =>
        sections.flatMap(section =>
          section.tasks.map((index, task) =>
            (page, TrailblazerTaskDecoder.decode(index, section.tier, area, task))
          )
        )
      }
      .via(errorReportingFlow(reportError))
      .map((_, task) => task)
  }

  private val pagesWithAreas: List[(LeagueTaskArea, Page.Name)] =
    List(
      LeagueTaskArea.Global -> "Trailblazer_League/Tasks/General",
      LeagueTaskArea.Misthalin -> "Trailblazer_League/Tasks/Misthalin",
      LeagueTaskArea.Karamja -> "Trailblazer_League/Tasks/Karamja",
      LeagueTaskArea.Asgarnia -> "Trailblazer_League/Tasks/Asgarnia",
      LeagueTaskArea.Fremennik -> "Trailblazer_League/Tasks/Fremennik_Provinces",
      LeagueTaskArea.Kandarin -> "Trailblazer_League/Tasks/Kandarin",
      LeagueTaskArea.Desert -> "Trailblazer_League/Tasks/Kharidian Desert",
      LeagueTaskArea.Morytania -> "Trailblazer_League/Tasks/Morytania",
      LeagueTaskArea.Tirannwn -> "Trailblazer_League/Tasks/Tirannwn",
      LeagueTaskArea.Wilderness -> "Trailblazer_League/Tasks/Wilderness",
    ).map((area, wikiName) => (area, Page.Name.from(wikiName)))

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
