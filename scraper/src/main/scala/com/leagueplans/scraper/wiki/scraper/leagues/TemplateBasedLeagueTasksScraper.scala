package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.scraper.wiki.decoder.DecoderResult
import com.leagueplans.scraper.wiki.decoder.leagues.rowextractors.TemplateBasedTaskRowExtractor
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiContentType, WikiSelector}
import com.leagueplans.scraper.wiki.parser.{Term, TermParser}
import com.leagueplans.scraper.wiki.streaming.*
import zio.{Trace, UIO}

object TemplateBasedLeagueTasksScraper {
  def make(
    client: WikiClient,
    selector: WikiSelector,
    templateName: String,
    decode: (Int, Term.Template.Object) => DecoderResult[LeagueTask]
  )(using Trace): UIO[TemplateBasedLeagueTasksScraper] =
    TaskIndexer.make.map(taskIndexer =>
      new TemplateBasedLeagueTasksScraper(client, taskIndexer, selector, templateName, decode)
    )
}

final class TemplateBasedLeagueTasksScraper(
  client: WikiClient,
  taskIndexer: TaskIndexer,
  selector: WikiSelector,
  templateName: String,
  decode: (Int, Term.Template.Object) => DecoderResult[LeagueTask]
) extends LeagueTasksScraper {
  private val extractor = TemplateBasedTaskRowExtractor(templateName)
  
  def scrape(using Trace): PageStream[LeagueTask] =
    client
      .fetch(selector, WikiContentType.Revisions)
      .pageMapEither(TermParser.parse)
      .pageMapEither(extractor)
      .pageFlattenIterables
      .pageMapZIO(task => taskIndexer.next.map((_, task)))
      .pageMapEither((index, task) => decode(index, task))
}
