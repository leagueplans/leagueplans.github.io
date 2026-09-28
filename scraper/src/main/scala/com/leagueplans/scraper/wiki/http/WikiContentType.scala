package com.leagueplans.scraper.wiki.http

enum WikiContentType(val prop: String) {
  case Revisions extends WikiContentType("revisions")

  /** When each page was last edited, as an ISO 8601 timestamp, rather than its content. */
  case LastEdited extends WikiContentType("revisions")
}
