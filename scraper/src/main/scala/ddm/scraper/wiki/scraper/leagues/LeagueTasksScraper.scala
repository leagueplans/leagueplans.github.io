package ddm.scraper.wiki.scraper.leagues

import ddm.common.model.LeagueTask
import ddm.scraper.wiki.streaming.PageStream
import zio.Trace

trait LeagueTasksScraper {
  def scrape(using Trace): PageStream[LeagueTask]
}
