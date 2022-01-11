package ddm.scraper.core.pages

import ddm.scraper.core.WikiBrowser
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._

final class FilePage[B <: Browser](wikiBrowser: WikiBrowser[B], wikiPath: String) {
  def fetchImage(): (String, Array[Byte]) = {
    val imgElement = wikiBrowser.fetchHtml(wikiPath) >> element("#file") >> element("img")
    val imagePath = imgElement.attr("src")
    val name = imgElement.attr("alt").replaceFirst("^File\\:", "")
    (name, wikiBrowser.fetchFile(imagePath))
  }
}
