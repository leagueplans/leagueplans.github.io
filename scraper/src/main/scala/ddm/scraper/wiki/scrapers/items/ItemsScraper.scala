package ddm.scraper.wiki.scrapers.items

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import ddm.scraper.wiki.WikiBrowser
import ddm.scraper.wiki.pages.{CategoryPage, ItemPage}
import ddm.scraper.wiki.scrapers.Scraper
import ddm.scraper.wiki.scrapers.utils.ImageStandardiser
import io.circe.syntax._
import net.ruippeixotog.scalascraper.browser.Browser
import org.log4s.{Logger, getLogger}

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.concurrent.{ExecutionContext, Future}

object ItemsScraper extends Scraper {
  private val logger: Logger = getLogger

  def run[B <: Browser](
    wikiBrowser: WikiBrowser[B],
    targetDirectory: Path
  )(implicit mat: Materializer, ec: ExecutionContext): Future[Unit] = {
    val imageLoader = ImmutableImage.loader()
    val dataPath = targetDirectory.resolve("data")
    val imagePath = targetDirectory.resolve("images/items")
    Files.createDirectories(dataPath)
    Files.createDirectories(imagePath)

    val fMaxDimensions =
      CategoryPage(wikiBrowser, "Items")
        .fetchPages((browser, path) => new ItemPage(browser, path))
        .map(_.fetchItemAndImage())
        .collect { case Some(itemAndImage) => itemAndImage }
        .runWith(Sink.fold((0, 0)) { case ((accMaxWidth, accMaxHeight), (item, rawImage)) =>
          val image = imageLoader.fromBytes(rawImage)
          val dimensions = image.dimensions()

          image.output(PngWriter.NoCompression, imagePath.resolve(s"${item.id}.png"))
          Files.write(
            dataPath.resolve("items"),
            item.asJson.noSpaces.getBytes(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
          )

          logger.info(s"Stored [${item.id}] [${item.name}]")

          (Math.max(accMaxWidth, dimensions.getX), Math.max(accMaxHeight, dimensions.getY))
        })

    fMaxDimensions.map { case (maxWidth, maxHeight) =>
      logger.info(s"Resizing icons")
      ImageStandardiser.resizeAll(imagePath, imageLoader, maxWidth, maxHeight)
    }
  }
}
