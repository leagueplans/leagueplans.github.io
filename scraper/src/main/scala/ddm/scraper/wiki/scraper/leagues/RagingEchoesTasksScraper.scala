package ddm.scraper.wiki.scraper.leagues

import ddm.scraper.wiki.decoder.leagues.RagingEchoesTaskDecoder
import ddm.scraper.wiki.http.{WikiClient, WikiSelector}
import ddm.scraper.wiki.model.PageDescriptor
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
