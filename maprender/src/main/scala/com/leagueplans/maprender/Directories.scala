package com.leagueplans.maprender

import java.nio.file.{Files, Path}
import java.util.Comparator
import scala.util.Using

object Directories {
  /** Deletes a file, or a directory and everything in it. Does nothing if the path doesn't exist. */
  def deleteRecursively(path: Path): Unit =
    if (Files.exists(path))
      Using.resource(Files.walk(path))(_.sorted(Comparator.reverseOrder()).forEach(Files.delete))
}
