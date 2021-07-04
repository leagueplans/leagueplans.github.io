package ddm.scraper.skillicons

import ddm.scraper.core.{PageFetcher, Scraper}
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.Path

object Main extends Scraper {
  def run(pageFetcher: PageFetcher[HtmlUnitBrowser], targetDirectory: Path): Unit =
    pageFetcher.fetch("File:Attack_icon.png#file")
}
