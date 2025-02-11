package com.leagueplans.scraper.wiki.http

import com.leagueplans.scraper.wiki.model.PageDescriptor

enum WikiSelector {
  case Pages(names: Vector[PageDescriptor.Name])
  case PagesThatTransclude(name: PageDescriptor.Name.Template)
  case Members(name: PageDescriptor.Name.Category)
}
