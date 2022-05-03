package ddm.scraper.wiki.http.response

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object Error {
  implicit val decoder: Decoder[Error] =
    deriveDecoder[Error]
}

final case class Error(code: String, info: String)
