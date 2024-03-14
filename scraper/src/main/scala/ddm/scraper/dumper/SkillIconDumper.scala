package ddm.scraper.dumper

import akka.stream.scaladsl.Sink

import java.nio.file.Path
import scala.concurrent.Future

object SkillIconDumper {
  def dump(imagesRootTarget: Path): Sink[(String, Array[Byte]), Future[?]] =
    imageSink(imagesRootTarget, targetWidth = 25, targetHeight = 25)
      .contramap[(String, Array[Byte])]((name, data) => Path.of(name) -> data)
}
