package ddm.scraper.wiki.scraper.leagues

import ddm.scraper.wiki.decoder.leagues.TwistedTaskDecoder
import ddm.scraper.wiki.http.{WikiClient, WikiSelector}
import ddm.scraper.wiki.model.PageDescriptor
import zio.{Trace, UIO}

object TwistedTasksScraper {
  def make(client: WikiClient)(using Trace): UIO[SectionBasedLeagueTasksScraper] =
    SectionBasedLeagueTasksScraper.make(
      client,
      WikiSelector.Pages(Vector(PageDescriptor.Name.from("Twisted_League/Tasks"))),
      TwistedTaskDecoder.decode
    )
}
