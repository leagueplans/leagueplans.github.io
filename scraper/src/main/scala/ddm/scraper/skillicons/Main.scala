package ddm.scraper.skillicons

import ddm.scraper.core.{PageFetcher, Scraper}
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.IterableHasAsJava

object Main extends Scraper {
  def run(pageFetcher: PageFetcher[HtmlUnitBrowser], targetDirectory: Path): Unit = {
    pageFetcher.fetch("File:Attack_icon.png#file")
    Files.write(
      targetDirectory.resolve("test.txt"),
      List("Some data to store in the file").asJava
    )
  }
}
