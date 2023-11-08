package ddm.scraper.wiki.scraper.leagues

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import ddm.common.model.{LeagueTask, LeagueTaskArea}
import ddm.scraper.wiki.decoder.leagues.{LeagueTaskRowExtractor, TrailblazerReloadedTaskDecoder}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.Page
import ddm.scraper.wiki.parser.TermParser

object TrailblazerReloadedTasksScraper {
  def scrape(
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[LeagueTask, _] = {
    val taskRowExtractor = new LeagueTaskRowExtractor

    Source(pagesWithAreas)
      .flatMapConcat { case (area, pageName) =>
        client
          .fetch(MediaWikiSelector.Pages(List(pageName)), Some(MediaWikiContent.Revisions))
          .map { case (page, result) => (page, result.map(content => (area, content))) }
      }
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
          section.tasks.map { case (index, task) =>
            (page, TrailblazerReloadedTaskDecoder.decode(index, section.tier, area, task))
          }
        )
      }
      .via(errorReportingFlow(reportError))
      .map { case (_, task) => task }
  }

  private val pagesWithAreas: List[(LeagueTaskArea, Page.Name)] =
    List(
      LeagueTaskArea.Global -> "Trailblazer_Reloaded_League/Tasks/General",
      LeagueTaskArea.Misthalin -> "Trailblazer_Reloaded_League/Tasks/Misthalin",
      LeagueTaskArea.Karamja -> "Trailblazer_Reloaded_League/Tasks/Karamja",
      LeagueTaskArea.Asgarnia -> "Trailblazer_Reloaded_League/Tasks/Asgarnia",
      LeagueTaskArea.Fremennik -> "Trailblazer_Reloaded_League/Tasks/Fremennik_Provinces",
      LeagueTaskArea.Kandarin -> "Trailblazer_Reloaded_League/Tasks/Kandarin",
      LeagueTaskArea.Kourend -> "Trailblazer_Reloaded_League/Tasks/Kourend_and_Kebos",
      LeagueTaskArea.Desert -> "Trailblazer_Reloaded_League/Tasks/Kharidian Desert",
      LeagueTaskArea.Morytania -> "Trailblazer_Reloaded_League/Tasks/Morytania",
      LeagueTaskArea.Tirannwn -> "Trailblazer_Reloaded_League/Tasks/Tirannwn",
      LeagueTaskArea.Wilderness -> "Trailblazer_Reloaded_League/Tasks/Wilderness",
    ).map { case (area, wikiName) => (area, Page.Name.from(wikiName)) }

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
