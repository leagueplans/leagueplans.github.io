package ddm.scraper.dumper

import akka.stream.scaladsl.Sink

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

object SkillIconDumper {
  def dump(imagesRootTarget: Path)(implicit ec: ExecutionContext): Sink[(String, Array[Byte]), Future[_]] =
    imageSink(imagesRootTarget, targetWidth = 25, targetHeight = 25)
      .contramap[(String, Array[Byte])] { case (name, data) => Path.of(name) -> data }
}
