package com.leagueplans.scrapereview

import com.leagueplans.scrapereview.items.dom.ReviewPage
import com.raquo.laminar.api.L
import org.scalajs.dom.document

/** Entry point for the tools that review what a scrape produced, before any of it is
  * accepted into the app's data.
  *
  * Item data is the only thing with a reviewer so far, so it is rendered directly. Combat
  * achievements and the collection log will both need one, and this becomes the place that
  * chooses between them.
  */
@main
def main(): Unit =
  L.renderOnDomContentLoaded(document.body, ReviewPage())
