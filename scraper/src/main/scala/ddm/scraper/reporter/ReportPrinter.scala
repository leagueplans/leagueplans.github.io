package ddm.scraper.reporter

import ddm.scraper.wiki.model.Page

import scala.util.{Failure, Success, Try}

object ReportPrinter {
  def print(runStatus: Try[_], failedPages: List[(Page, Throwable)], baseURL: String): String =
    s"${printRunStatus(runStatus)}${printFailedPages(failedPages, baseURL)}"

  private def printRunStatus(status: Try[_]): String =
    status match {
      case Success(_) =>
        """### Run status
          |✅ **COMPLETE**
          |""".stripMargin

      case Failure(cause) =>
        s"""### Run status
           |❌ **ERROR**
           |```
           |$cause
           |```
           |""".stripMargin
    }

  private def printFailedPages(failures: List[(Page, Throwable)], baseURL: String): String =
    if (failures.isEmpty)
      ""
    else
      s"""
         |### Failed pages
         |${failures.map { case (page, error) => printFailedPage(page, error, baseURL) }.mkString("\n")}
         |""".stripMargin

  private def printFailedPage(page: Page, error: Throwable, baseURL: String): String = {
    val replaced = page.name.wikiName.replace(' ', '_')
    s"""[${page.name.wikiName}]($baseURL/w/$replaced) (Page ID ${page.id.raw})
       |```
       |$error
       |```
       |""".stripMargin
  }
}
