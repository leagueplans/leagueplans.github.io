package com.leagueplans.scraper.wiki.model

import io.circe.{Decoder, Encoder}

object InfoboxVersion {
  given Encoder[InfoboxVersion] = Encoder[List[String]].contramap(_.raw)
  given Decoder[InfoboxVersion] = Decoder[List[String]].map(InfoboxVersion.apply)

  given Ordering[InfoboxVersion] =
    Ordering.by[InfoboxVersion, List[String]](_.raw)(using Ordering.Implicits.seqOrdering)
}

final case class InfoboxVersion(raw: List[String]) extends AnyVal {
  def isSubVersionOf(other: InfoboxVersion): Boolean =
    other.raw.forall(raw.contains)
}
