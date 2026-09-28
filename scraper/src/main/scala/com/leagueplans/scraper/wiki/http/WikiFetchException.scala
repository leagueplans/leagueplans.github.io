package com.leagueplans.scraper.wiki.http

import com.leagueplans.scraper.wiki.http.response.WikiResponse
import com.leagueplans.scraper.wiki.model.PageDescriptor
import zio.http.Status

sealed abstract class WikiFetchException(message: String) extends RuntimeException(message)

object WikiFetchException {
  final case class HTTPError(status: Status, body: String) extends WikiFetchException(
    s"${status.code} ${status.reasonPhrase}, body = [$body]"
  )

  final case class ErrorResponse(failure: WikiResponse.Failure) extends WikiFetchException(
    s"${failure.error.code}: ${failure.error.info}"
  )

  final case class NoFileInfo(file: PageDescriptor.Name.File) extends WikiFetchException(
    s"The wiki reported no SHA-1 for [${file.wikiName}]"
  )
}
