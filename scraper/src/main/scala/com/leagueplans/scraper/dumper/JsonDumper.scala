package com.leagueplans.scraper.dumper

import io.circe.Encoder
import io.circe.syntax.EncoderOps

import java.nio.file.{Files, Path}
import scala.util.Try

object JsonDumper {
  def make[T : Encoder](target: Path): Try[JsonDumper[T]] =
    Try(Files.createFile(target)).map(JsonDumper(_))
}

final class JsonDumper[T : Encoder](target: Path) {
  def dump(data: T): Try[Unit] =
    Try {
      Files.write(target, data.asJson.noSpaces.getBytes)
      ()
    }
}
