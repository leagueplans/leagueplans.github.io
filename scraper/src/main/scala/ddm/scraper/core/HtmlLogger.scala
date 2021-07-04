package ddm.scraper.core

import net.ruippeixotog.scalascraper.model.Document
import org.apache.commons.io.file.PathUtils
import org.log4s.{Logger, getLogger}

import java.nio.file.{Files, Path}
import java.time.{Clock, Instant}
import scala.jdk.CollectionConverters.IterableHasAsJava

object HtmlLogger {
  def prepare(logDirectory: Path, clock: Clock): HtmlLogger = {
    Files.createDirectories(logDirectory)
    PathUtils.cleanDirectory(logDirectory)
    new HtmlLogger(logDirectory, clock)
  }
}

final class HtmlLogger(logDirectory: Path, clock: Clock) {
  private val logger: Logger = getLogger

  def log(doc: Document): Unit = {
    val timestamp = Instant.now(clock).toString.replace(':', '-')
    val filePath = logDirectory.resolve(s"$timestamp.html")
    Files.write(filePath, List(doc.toHtml).asJava)
    logger.info(s"Saved HTML to [$filePath]")
  }
}
