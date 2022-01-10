package ddm.scraper.core

import org.log4s.{Logger, getLogger}

import java.nio.file.{Files, Path}

object FileStore {
  def prepare(directory: Path): FileStore = {
    Files.createDirectories(directory)
    new FileStore(directory)
  }
}

final class FileStore(directory: Path) {
  private val logger: Logger = getLogger

  def persist(wikiPath: String, data: Array[Byte]): Unit = {
    Files.write(resolve(wikiPath), data)
    logger.info(s"Saved to file system [$wikiPath]")
  }

  def recover(wikiPath: String): Option[Array[Byte]] = {
    val filePath = resolve(wikiPath)
    Option.when(Files.exists(filePath))(
      Files.readAllBytes(filePath)
    )
  }

  private def resolve(wikiPath: String): Path =
    directory.resolve(
      s"${wikiPath.replaceAll("\\W+", "")}.html"
    )
}
