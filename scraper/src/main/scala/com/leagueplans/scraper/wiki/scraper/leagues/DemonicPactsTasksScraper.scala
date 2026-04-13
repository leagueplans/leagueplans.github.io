package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.scraper.wiki.decoder.leagues.DemonicPactsTaskDecoder
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
import zio.{Trace, UIO}

object DemonicPactsTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[TemplateBasedLeagueTasksScraper] =
    TemplateBasedLeagueTasksScraper.make(
      client,
      WikiSelector.Pages(Vector(PageDescriptor.Name.from("Demonic_Pacts_League/Tasks"))),
      templateName = "dpltaskrow",
      DemonicPactsTaskDecoder.decode
    )
}
