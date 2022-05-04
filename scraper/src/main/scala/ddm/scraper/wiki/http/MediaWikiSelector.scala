package ddm.scraper.wiki.http

import ddm.scraper.wiki.model.Page

sealed trait MediaWikiSelector

object MediaWikiSelector {
  final case class Pages(names: List[Page.Name]) extends MediaWikiSelector
  final case class PagesThatTransclude(name: Page.Name.Template) extends MediaWikiSelector
  final case class Members(name: Page.Name.Category) extends MediaWikiSelector
}