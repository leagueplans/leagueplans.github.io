package ddm.scraper.scrapers.items

import com.sksamuel.scrimage.ImmutableImage
import ddm.scraper.core.pages.{CategoryPage, ItemPage}
import ddm.scraper.core.{Scraper, WikiBrowser}
import ddm.scraper.scrapers.utils.ImagePrinter
import io.circe.JsonObject
import io.circe.syntax._
import net.ruippeixotog.scalascraper.browser.HtmlUnitBrowser

import java.nio.file.{Files, Path}

object ItemsScraper extends Scraper {
  def run(
    pageFetcher: WikiBrowser[HtmlUnitBrowser],
    targetDirectory: Path
  ): Unit = {
    val imageLoader = ImmutableImage.loader()
    val dataPath = targetDirectory.resolve("data")
    val imagePath = targetDirectory.resolve("images/items")
    Files.createDirectories(dataPath)
    Files.createDirectories(imagePath)

    val items =
      CategoryPage(pageFetcher, "Items")
        .fetchPages((browser, path) => new ItemPage(browser, browser.fetchHtml(path)))
        .flatMap(_.fetchItem())

    val itemsToImages = items.map(item => item -> imageLoader.fromBytes(item.image))

    ImagePrinter.print(itemsToImages) { case (_, image) => image } { case (item, _) =>
      imagePath.resolve(s"${item.id}.png")
    }

    val data =
      items
        .sortBy(_.id)
        .map(item =>
          JsonObject(
            "id" -> item.id.asJson,
            "name" -> item.name.asJson,
            "examine" -> item.examine.asJson,
            "stackable" -> item.stackable.asJson
          )
        ).asJson

    Files.write(dataPath.resolve("items"), data.noSpaces.getBytes())
  }
}
