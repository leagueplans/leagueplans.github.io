package ddm.scraper.wiki.scraper.leagues

import ddm.scraper.wiki.decoder.leagues.TrailblazerReloadedTaskDecoder
import ddm.scraper.wiki.http.{WikiClient, WikiSelector}
import ddm.scraper.wiki.model.PageDescriptor
import zio.{Trace, UIO}

object TrailblazerReloadedTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[TemplateBasedLeagueTasksScraper] =
    TemplateBasedLeagueTasksScraper.make(
      client,
      WikiSelector.Pages(Vector(PageDescriptor.Name.from("Trailblazer_Reloaded_League/Tasks"))),
      templateName = "trltaskrow",
      TrailblazerReloadedTaskDecoder.decode
    )
}
