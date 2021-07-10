package ddm.scraper.skillicons

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import ddm.scraper.core.pages.CategoryPage
import ddm.scraper.core.{Scraper, WikiFetcher}
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.Path

object SkillIconsScraper extends Scraper {
  def run(
    pageFetcher: WikiFetcher[HtmlUnitBrowser],
    targetDirectory: Path
  ): Unit = {
    val imageLoader = ImmutableImage.loader()

    val files =
      CategoryPage(pageFetcher, "Skill_icons")
        .fetchFilePages()
        .map { page =>
          val (name, bytes) = page.fetchImage()
          (name, imageLoader.fromBytes(bytes))
        }
        .filter { case (name, _) => name.contains("icon") }

    val (_, images) = files.unzip
    val (maxWidth, maxHeight) = findMaxDimensions(images)

    files.foreach { case (name, image) =>
      val simplifiedName = name.replaceFirst(" icon", "")

      image
        .resizeTo(maxWidth, maxHeight)
        .output(PngWriter.NoCompression, targetDirectory.resolve(simplifiedName))
    }
  }

  private def findMaxDimensions(images: List[ImmutableImage]): (Int, Int) =
    images.foldLeft((0, 0)) { case ((widthAcc, heightAcc), image) =>
      val dimensions = image.dimensions()
      (Math.max(widthAcc, dimensions.getX), Math.max(heightAcc, dimensions.getY))
    }
}
