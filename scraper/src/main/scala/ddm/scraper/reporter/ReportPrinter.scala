package ddm.scraper.reporter

import scala.util.{Failure, Success, Try}

object ReportPrinter {
  def print(runStatus: Try[_], failedPages: List[Reporter.Message.Failure], baseURL: String): String =
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

  private def printFailedPages(pages: List[Reporter.Message.Failure], baseURL: String): String =
    if (pages.isEmpty)
      ""
    else
      s"""
         |### Failed pages
         |${pages.map(printFailedPage(_, baseURL)).mkString("\n")}
         |""".stripMargin

  private def printFailedPage(failure: Reporter.Message.Failure, baseURL: String): String = {
    val replaced = failure.page.name.wikiName.replace(' ', '_')
    s"""[${failure.page.name.wikiName}]($baseURL/w/$replaced) (Page ID ${failure.page.id.raw})
       |```
       |${failure.cause}
       |```
       |""".stripMargin
  }
}
