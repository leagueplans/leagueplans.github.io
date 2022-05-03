package ddm.scraper

import akka.stream.scaladsl.Sink
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.circe.Encoder
import io.circe.syntax.EncoderOps

import java.nio.file.{Files, OpenOption, Path}
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

package object dumper {
  def dataSink[T : Encoder](
    path: Path,
    options: OpenOption*
  )(implicit ec: ExecutionContext): Sink[T, Future[_]] =
    Sink.lazySink { () =>
      val writer = Files.newOutputStream(path, options: _*)

      Sink
        .fold[Boolean, T](true) { (isFirstElement, element) =>
          val data = element.asJson.noSpaces

          if (!isFirstElement)
            writer.write(s",$data".getBytes)
          else
            writer.write(s"[$data".getBytes)

          false
        }
        // There's always first element due to lazy creation, so we can always write a `]`
        .mapMaterializedValue(_.map(_ => writer.write("]".getBytes)))
        .mapMaterializedValue(_.onComplete(_ => writer.close()))
    }

  def imageSink(
    rootPath: Path,
    targetWidth: Int,
    targetHeight: Int
  ): Sink[(Path, Array[Byte]), Future[_]] =
    Sink.lazySink { () =>
      val imageLoader = ImmutableImage.loader()

      Sink.foreach[(Path, Array[Byte])] { case (subPath, data) =>
        val path = rootPath.resolve(subPath)
        Files.createDirectories(path.getParent)

        // Potential issue here - what if the file wasn't a PNG?
        imageLoader
          .fromBytes(data)
          .resizeTo(targetWidth, targetHeight)
          .output(PngWriter.MaxCompression, path): @nowarn("msg=discarded non-Unit value")
      }
    }.mapMaterializedValue(_.flatten)
}
