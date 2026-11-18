package com.leagueplans.maprender

import java.nio.file.{Files, Path}

object TempDirectory {
  /** Runs `use` with a new, empty directory, and deletes the directory afterwards */
  def apply[T](prefix: String)(use: Path => T): T = {
    val directory = Files.createTempDirectory(prefix)
    try use(directory)
    finally Directories.deleteRecursively(directory)
  }
}
