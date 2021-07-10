package ddm.scraper.core

import org.log4s.{Logger, getLogger}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

object PageLogger {
  def prepare(logDirectory: Path): PageLogger = {
    Files.createDirectories(logDirectory)
    new PageLogger(logDirectory)
  }
}

final class PageLogger(logDirectory: Path) {
  private val logger: Logger = getLogger

  def logHtml(wikiPath: String, data: String): Unit = {
    Files.write(resolve(wikiPath), List(data).asJava)
    logger.info(s"Saved data to file system")
  }

  def recoverHtml(wikiPath: String): Option[String] = {
    val filePath = resolve(wikiPath)
    Option.when(Files.exists(filePath))(
      Files
        .readAllLines(filePath)
        .asScala
        .mkString("")
    )
  }

  private def resolve(wikiPath: String): Path =
    logDirectory.resolve(
      s"${wikiPath.replaceAll("\\W+", "")}.html"
    )
}
