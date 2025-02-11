package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.scraper.wiki.decoder.leagues.RagingEchoesTaskDecoder
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
import zio.{Trace, UIO}

object RagingEchoesTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[TemplateBasedLeagueTasksScraper] =
    TemplateBasedLeagueTasksScraper.make(
      client,
      WikiSelector.Pages(Vector(PageDescriptor.Name.from("Raging_Echoes_League/Tasks"))),
      templateName = "reltaskrow",
      RagingEchoesTaskDecoder.decode
    )
}
