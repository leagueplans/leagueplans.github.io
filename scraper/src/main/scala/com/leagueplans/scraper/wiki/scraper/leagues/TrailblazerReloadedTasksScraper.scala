package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.scraper.wiki.decoder.leagues.TrailblazerReloadedTaskDecoder
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
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
