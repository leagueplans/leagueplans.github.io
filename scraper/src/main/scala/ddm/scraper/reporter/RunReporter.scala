package ddm.scraper.reporter

import ddm.scraper.wiki.model.PageDescriptor
import ddm.scraper.wiki.streaming.PageStream
import zio.http.{Request, URL}
import zio.{Chunk, Exit}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.util.Try

object RunReporter {
  def make(baseURL: URL, target: Path): Try[RunReporter] =
    Try(Files.createFile(target)).map(RunReporter(baseURL, _))
}

final class RunReporter(baseURL: URL, target: Path) {
  def report(result: Exit[Throwable, Chunk[PageStream.Error]]): Try[Unit] =
    Try {
      Files.write(target, format(result).getBytes(StandardCharsets.UTF_8))
      ()
    }

  private def format(result: Exit[Throwable, Chunk[PageStream.Error]]): String = {
    val contents = result match {
      case Exit.Success(errors) =>
        s"""✅ **COMPLETE**
           |
           |${formatFailures(errors)}""".stripMargin

      case Exit.Failure(cause) =>
        s"""❌ **ERROR**
           |```text
           |${cause.prettyPrint}
           |```""".stripMargin
    }

    s"""## 🏃‍➡️ Run status
       |$contents""".stripMargin
  }

  private def formatFailures(errors: Chunk[PageStream.Error]): String = {
    val (failedRequests, failedPages) = errors.partitionMap {
      case (request: Request, error) => Left(request, error)
      case (page: PageDescriptor, error) => Right(page, error)
    }

    s"""${formatFailedRequests(failedRequests)}
       |
       |${formatFailedPages(failedPages)}""".stripMargin
  }

  private def formatFailedRequests(errors: Chunk[(Request, Throwable)]): String = {
    val contents =
      errors.map((request, error) =>
        s"${formatFailedRequest(request, error)}"
      ).mkString("\n---\n")

    s"""## 📡 Failed requests
       |$contents""".stripMargin
  }

  private def formatFailedRequest(request: Request, error: Throwable): String =
    s"""`${request.method.render} ${request.url.encode}`
       |${formatError(error)}""".stripMargin

  private def formatFailedPages(errors: Chunk[(PageDescriptor, Throwable)]): String = {
    val contents =
      errors.map((page, error) =>
        s"${formatFailedPage(page, error)}"
      ).mkString("\n---\n")

    s"""## 📄 Failed pages
       |$contents""".stripMargin
  }

  private def formatFailedPage(page: PageDescriptor, error: Throwable): String =
    s"""${formatPage(page)}
       |${formatError(error)}""".stripMargin

  private def formatPage(page: PageDescriptor): String = {
    // Required for proper markdown link rendering
    val replaced = page.name.wikiName.replace(' ', '_')
    s"[${page.name.wikiName}](${baseURL.encode}/w/$replaced) (Page ID ${page.id})"
  }

  private def formatError(error: Throwable): String =
    s"""```text
       |$error
       |```""".stripMargin
}
