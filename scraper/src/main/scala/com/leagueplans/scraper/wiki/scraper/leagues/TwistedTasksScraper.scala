package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.scraper.wiki.decoder.leagues.TwistedTaskDecoder
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiSelector}
import com.leagueplans.scraper.wiki.model.PageDescriptor
import zio.{Trace, UIO}

object TwistedTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[SectionBasedLeagueTasksScraper] =
    SectionBasedLeagueTasksScraper.make(
      client,
      WikiSelector.Pages(Vector(PageDescriptor.Name.from("Twisted_League/Tasks"))),
      TwistedTaskDecoder.decode
    )
}
