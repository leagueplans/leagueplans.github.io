package ddm.scraper.wiki.scrapers.equipmenticons

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import ddm.scraper.wiki.pages.CategoryPage
import ddm.scraper.wiki.scrapers.utils.ImageStandardiser
import ddm.scraper.wiki.WikiBrowser
import ddm.scraper.wiki.scrapers.Scraper
import net.ruippeixotog.scalascraper.browser.Browser

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

object EquipmentIconsScraper extends Scraper {
  def run[B <: Browser](
    wikiBrowser: WikiBrowser[B],
    targetDirectory: Path
  )(implicit mat: Materializer, ec: ExecutionContext): Future[Unit] = {
    val imageLoader = ImmutableImage.loader()

    val fMaxDimensions =
      CategoryPage(wikiBrowser, "Equipment_slot_icons")
        .fetchFilePages()
        .map(_.fetchImage())
        .runWith(Sink.fold((0, 0)) { case ((accMaxWidth, accMaxHeight), (name, rawImage)) =>
          val image = imageLoader.fromBytes(rawImage)
          val dimensions = image.dimensions()

          image.output(PngWriter.NoCompression, targetDirectory.resolve(name))

          (Math.max(accMaxWidth, dimensions.getX), Math.max(accMaxHeight, dimensions.getY))
        })

    fMaxDimensions.map { case (maxWidth, maxHeight) =>
      ImageStandardiser.resizeAll(targetDirectory, imageLoader, maxWidth, maxHeight)
    }
  }
}
