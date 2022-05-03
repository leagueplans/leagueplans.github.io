package ddm.scraper.wiki.http

sealed trait MediaWikiContent

object MediaWikiContent {
  case object Revisions extends MediaWikiContent
}
