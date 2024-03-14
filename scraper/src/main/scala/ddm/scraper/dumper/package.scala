package ddm.scraper.dumper

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.typed.scaladsl.ActorSink
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.circe.Encoder

import java.nio.file.{Files, Path}
import scala.annotation.nowarn
import scala.concurrent.Future
import scala.util.{Failure, Success}

def dataSink[T : Encoder : Ordering](cache: ActorRef[Cache.Message[T]]): Sink[T, ?] =
  Flow[T]
    .map(Cache.Message.NewEntry(_))
    .to(ActorSink.actorRef(
      cache,
      onCompleteMessage = Cache.Message.Complete(Success(())),
      onFailureMessage = cause => Cache.Message.Complete(Failure(cause))
    ))

def imageSink(
  rootPath: Path,
  targetWidth: Int,
  targetHeight: Int
): Sink[(Path, Array[Byte]), Future[?]] =
  Sink.lazySink { () =>
    val imageLoader = ImmutableImage.loader()

    Sink.foreach[(Path, Array[Byte])] { (subPath, data) =>
      val path = rootPath.resolve(subPath)
      Files.createDirectories(path.getParent)

      // Potential issue here - what if the file wasn't a PNG?
      imageLoader
        .fromBytes(data)
        .resizeTo(targetWidth, targetHeight)
        .output(PngWriter.MaxCompression, path): @nowarn("msg=discarded non-Unit value")
    }
  }.mapMaterializedValue(_.flatten)
