package com.leagueplans.scraper.reporter

import com.leagueplans.scraper.wiki.model.PageDescriptor
import com.leagueplans.scraper.wiki.streaming.PageStream
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
           |${formatFailureSections(errors)}""".stripMargin

      case Exit.Failure(cause) =>
        s"""❌ **ERROR**
           |```text
           |${cause.prettyPrint}
           |```""".stripMargin
    }

    s"""## 🏃‍➡️ Run status
       |$contents""".stripMargin
  }

  private def formatFailureSections(errors: Chunk[PageStream.Error]): String = {
    val (failedRequests, failedPages) = errors.partitionMap {
      case (request: Request, error) => Left(request, error)
      case (page: PageDescriptor, error) => Right(page, error)
    }

    s"""${formatFailedRequests(failedRequests)}
       |
       |${formatFailedPages(failedPages)}
       |""".stripMargin
  }

  private def formatFailedRequests(errors: Chunk[(Request, Throwable)]): String =
    s"""## 📡 Failed requests
       |${formatFailures(errors)(formatRequest)(using Ordering.by(_.url.encode))}""".stripMargin

  private def formatRequest(request: Request): String =
    s"`${request.method.render} ${request.url.encode}`"

  private def formatFailedPages(errors: Chunk[(PageDescriptor, Throwable)]): String =
    s"""## 📄 Failed pages
       |${formatFailures(errors)(formatPage)}""".stripMargin

  private def formatPage(page: PageDescriptor): String = {
    // Required for proper markdown link rendering
    val replaced = page.name.wikiName.replace(' ', '_')
    s"[${page.name.wikiName}](${baseURL.encode}/w/$replaced) (Page ID ${page.id})"
  }

  private def formatFailures[ID : Ordering](
    errors: Chunk[(ID, Throwable)]
  )(formatID: ID => String): String =
    errors
      .groupMap((_, error) => formatError(error))((id, _) => id)
      .toVector
      .sortBy((error, _) => error)
      .map((error, ids) =>
        s"""$error
           |${ids.sorted.map(formatID).mkString("\n\n")}""".stripMargin
      ).mkString("\n\n---\n")

  private def formatError(error: Throwable): String =
    s"""```text
       |$error
       |```""".stripMargin
}
