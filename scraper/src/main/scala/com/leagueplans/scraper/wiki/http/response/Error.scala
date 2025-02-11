package com.leagueplans.scraper.wiki.http.response

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

private[response] object Error {
  given Decoder[Error] = deriveDecoder[Error]
}

private[response] final case class Error(code: String, info: String)
