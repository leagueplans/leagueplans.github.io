package com.leagueplans.scraper.wiki.scraper.leagues

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.scraper.wiki.streaming.PageStream
import zio.Trace

trait LeagueTasksScraper {
  def scrape(using Trace): PageStream[LeagueTask]
}
