package ddm.scraper.skillicons

import ddm.scraper.core.{PageFetcher, Scraper}
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

object Main extends Scraper {
  def run(pageFetcher: PageFetcher[HtmlUnitBrowser]): Unit =
    pageFetcher.fetch("File:Attack_icon.png#file")
}
