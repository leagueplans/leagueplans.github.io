package ddm.scraper.dumper

import akka.actor.typed.Behavior
import io.circe.Encoder
import io.circe.syntax.EncoderOps

import java.nio.file.{Files, OpenOption, Path}
import scala.annotation.nowarn

object CachingWriter {
  def to[T : Encoder : Ordering](path: Path, options: OpenOption*): Behavior[Cache.Message[T]] =
    Cache.init { (_, entries) =>
      Files.createDirectories(path.getParent)
      Files.write(
        path,
        entries.sorted.asJson.noSpaces.getBytes,
        options: _*
      ): @nowarn("msg=discarded non-Unit value")
    }
}
