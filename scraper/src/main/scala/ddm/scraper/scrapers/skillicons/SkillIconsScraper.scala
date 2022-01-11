package ddm.scraper.scrapers.skillicons

import com.sksamuel.scrimage.ImmutableImage
import ddm.scraper.core.pages.CategoryPage
import ddm.scraper.core.{Scraper, WikiBrowser}
import ddm.scraper.scrapers.utils.ImagePrinter
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.Path

object SkillIconsScraper extends Scraper {
  def run(
    pageFetcher: WikiBrowser[HtmlUnitBrowser],
    targetDirectory: Path
  ): Unit = {
    val imageLoader = ImmutableImage.loader()

    val files =
      CategoryPage(pageFetcher, "Skill_icons")
        .recurse(_.fetchFilePages())
        .map { page =>
          val (name, bytes) = page.fetchImage()
          (name, imageLoader.fromBytes(bytes))
        }
        .filter { case (name, _) => name.contains("icon") }

    ImagePrinter.print(files) { case (_, image) => image } { case (name, _) =>
      val simplifiedName = name.replaceFirst(" icon", "")
      targetDirectory.resolve(simplifiedName)
    }
  }
}
