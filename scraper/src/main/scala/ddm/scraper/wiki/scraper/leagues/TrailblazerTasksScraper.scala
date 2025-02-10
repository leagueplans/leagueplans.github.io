package ddm.scraper.wiki.scraper.leagues

import ddm.common.model.{LeagueTask, LeagueTaskArea}
import ddm.scraper.telemetry.WithStreamAnnotation
import ddm.scraper.wiki.decoder.leagues.TrailblazerTaskDecoder
import ddm.scraper.wiki.http.{WikiClient, WikiSelector}
import ddm.scraper.wiki.model.PageDescriptor
import ddm.scraper.wiki.streaming.PageStream
import zio.stream.ZStream
import zio.{Trace, UIO}

object TrailblazerTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[TrailblazerTasksScraper] =
    TaskIndexer.make.map(taskIndexer =>
      new TrailblazerTasksScraper(client, taskIndexer)
    )

  private val areaSelectors: Vector[(LeagueTaskArea, WikiSelector)] =
    Vector(
      LeagueTaskArea.Global -> "Trailblazer_League_(2020)/Tasks/General",
      LeagueTaskArea.Misthalin -> "Trailblazer_League_(2020)/Tasks/Misthalin",
      LeagueTaskArea.Karamja -> "Trailblazer_League_(2020)/Tasks/Karamja",
      LeagueTaskArea.Asgarnia -> "Trailblazer_League_(2020)/Tasks/Asgarnia",
      LeagueTaskArea.Fremennik -> "Trailblazer_League_(2020)/Tasks/Fremennik_Provinces",
      LeagueTaskArea.Kandarin -> "Trailblazer_League_(2020)/Tasks/Kandarin",
      LeagueTaskArea.Desert -> "Trailblazer_League_(2020)/Tasks/Kharidian Desert",
      LeagueTaskArea.Morytania -> "Trailblazer_League_(2020)/Tasks/Morytania",
      LeagueTaskArea.Tirannwn -> "Trailblazer_League_(2020)/Tasks/Tirannwn",
      LeagueTaskArea.Wilderness -> "Trailblazer_League_(2020)/Tasks/Wilderness",
    ).map((area, wikiName) => (area, WikiSelector.Pages(Vector(PageDescriptor.Name.from(wikiName)))))
}

final class TrailblazerTasksScraper(client: WikiClient, taskIndexer: TaskIndexer) extends LeagueTasksScraper {
  def scrape(using Trace): PageStream[LeagueTask] =
    ZStream
      .fromIterable(TrailblazerTasksScraper.areaSelectors)
      .flatMapPar(n = 4)((area, selector) =>
        WithStreamAnnotation.forLogs("task-area" -> area.name)(
          SectionBasedLeagueTasksScraper(
            client,
            taskIndexer,
            selector,
            TrailblazerTaskDecoder.decode(_, _, area, _)
          ).scrape
        )
      )
}
