package ddm.scraper.core.pages

import ddm.scraper.core.WikiFetcher
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

final class FilePage[B <: Browser](
  pageFetcher: WikiFetcher[B],
  currentPage: B#DocumentType,
) {
  def fetchImage(): (String, Array[Byte]) = {
    val imgElement = currentPage >> element("#file") >> element("img")
    val wikiPath = imgElement.attr("src")
    val name = imgElement.attr("alt").replaceFirst("^File\\:", "")
    (name, pageFetcher.fetchFile(wikiPath))
  }
}
