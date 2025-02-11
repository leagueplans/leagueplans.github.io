package ddm.scraper.dumper

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.{ImmutableImageLoader, PngWriter}
import ddm.scraper.telemetry.Metric
import zio.{Task, Trace, ZIO}

import java.nio.file.{Files, Path}

object ImageDumper {
  def make[T](name: String, targetDirectory: Path)(using Trace): Task[ImageDumper] =
    for {
      _ <- ZIO.attempt(Files.createDirectories(targetDirectory))
      imageCounter <- Metric.makeCounter(s"$name.image-dumper.images")
      byteCounter <- Metric.makeCounter(s"$name.image-dumper.bytes")
    } yield ImageDumper(ImmutableImage.loader(), targetDirectory, imageCounter, byteCounter)
}

final class ImageDumper(
  imageLoader: ImmutableImageLoader,
  directory: Path,
  imageCounter: Metric.Counter[Long],
  byteCounter: Metric.Counter[Long]
) {
  def dump(subPath: Path, data: Array[Byte])(using Trace): Task[Unit] =
    ZIO.uninterruptibleMask(restore =>
      for {
        _ <- restore(writeToFile(subPath, data))
        _ <- imageCounter.increment
        _ <- byteCounter.incrementBy(data.length)
      } yield ()
    )
    
  private def writeToFile(subPath: Path, data: Array[Byte])(using Trace): Task[Unit] =
    ZIO.attempt {
      val path = directory.resolve(subPath)
      Files.createDirectories(path.getParent)
      imageLoader.fromBytes(data).output(PngWriter.MaxCompression, path)
      ()
    }
}
