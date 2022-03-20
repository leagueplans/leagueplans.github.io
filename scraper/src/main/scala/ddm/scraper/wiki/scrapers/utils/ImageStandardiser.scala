package ddm.scraper.wiki.scrapers.utils

import com.sksamuel.scrimage.nio.{ImmutableImageLoader, PngWriter}

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.SetHasAsJava

object ImageStandardiser {
  def resizeAll(
    directory: Path,
    imageLoader: ImmutableImageLoader,
    targetWidth: Int,
    targetHeight: Int
  ): Unit =
    Files.walkFileTree(
      directory,
      Set.empty[FileVisitOption].asJava,
      /* maxDepth = */ 1,
      new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          imageLoader
            .fromBytes(Files.readAllBytes(file))
            .resizeTo(targetWidth, targetHeight)
            .output(PngWriter.NoCompression, file)

          FileVisitResult.CONTINUE
        }
      }
    ): @nowarn("msg=discarded non-Unit value")
}
