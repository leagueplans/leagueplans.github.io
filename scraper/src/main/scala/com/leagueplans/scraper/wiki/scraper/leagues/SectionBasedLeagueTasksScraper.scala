package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.common.model.{LeagueTask, LeagueTaskTier}
import com.leagueplans.scraper.wiki.decoder.DecoderResult
import com.leagueplans.scraper.wiki.decoder.leagues.rowextractors.SectionBasedTaskRowExtractor
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiContentType, WikiSelector}
import com.leagueplans.scraper.wiki.parser.{Term, TermParser}
import com.leagueplans.scraper.wiki.streaming.*
import zio.{Trace, UIO}

object SectionBasedLeagueTasksScraper {
  def make[EncodedTask](
    client: WikiClient,
    selector: WikiSelector,
    decode: (Int, LeagueTaskTier, Term.Template) => DecoderResult[LeagueTask]
  )(using Trace): UIO[SectionBasedLeagueTasksScraper] =
    TaskIndexer.make.map(taskIndexer =>
      new SectionBasedLeagueTasksScraper(client, taskIndexer, selector, decode)
    )
}

final class SectionBasedLeagueTasksScraper(
  client: WikiClient,
  taskIndexer: TaskIndexer,
  selector: WikiSelector,
  decode: (Int, LeagueTaskTier, Term.Template) => DecoderResult[LeagueTask]
) extends LeagueTasksScraper {
  def scrape(using Trace): PageStream[LeagueTask] =
    client
      .fetch(selector, WikiContentType.Revisions)
      .pageMapEither(TermParser.parse)
      .pageMapEither(SectionBasedTaskRowExtractor.extract)
      .pageFlattenIterables
      .pageMap(section => section.tasks.map((section.tier, _)))
      .pageFlattenIterables
      .pageMapZIO((tier, task) => taskIndexer.next.map((_, tier, task)))
      .pageMapEither((index, tier, task) => decode(index, tier, task))
}
