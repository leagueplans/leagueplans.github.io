package ddm.scraper.scrapers.utils

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter

import java.nio.file.Path

object ImagePrinter {
  def print[T](ts: List[T])(toImage: T => ImmutableImage)(toPath: T => Path): Unit = {
    val images = ts.map(toImage)
    val (maxWidth, maxHeight) = findMaxDimensions(images)

    ts.foreach(t =>
      toImage(t)
        .resizeTo(maxWidth, maxHeight)
        .output(PngWriter.NoCompression, toPath(t))
    )
  }

  private def findMaxDimensions(images: List[ImmutableImage]): (Int, Int) =
    images.foldLeft((0, 0)) { case ((widthAcc, heightAcc), image) =>
      val dimensions = image.dimensions()
      (Math.max(widthAcc, dimensions.getX), Math.max(heightAcc, dimensions.getY))
    }
}
