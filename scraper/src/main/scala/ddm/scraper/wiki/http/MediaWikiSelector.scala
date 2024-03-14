package ddm.scraper.wiki.http

import ddm.scraper.wiki.model.Page

enum MediaWikiSelector {
  case Pages(names: List[Page.Name])
  case PagesThatTransclude(name: Page.Name.Template)
  case Members(name: Page.Name.Category)
}
