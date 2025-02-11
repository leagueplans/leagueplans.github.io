package com.leagueplans.scraper.wiki.http

import com.leagueplans.scraper.wiki.http.response.WikiResponse
import zio.http.Status

sealed abstract class WikiFetchException(message: String) extends RuntimeException(message)

object WikiFetchException {
  final case class HTTPError(status: Status, body: String) extends WikiFetchException(
    s"${status.code} ${status.reasonPhrase}, body = [$body]"
  )

  final case class ErrorResponse(failure: WikiResponse.Failure) extends WikiFetchException(
    s"${failure.error.code}: ${failure.error.info}"
  )
}
