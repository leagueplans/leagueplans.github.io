package ddm.scraper.wiki.model

import io.circe.{Decoder, Encoder}

object InfoboxVersion {
  implicit val encoder: Encoder[InfoboxVersion] = Encoder[List[String]].contramap(_.raw)
  implicit val decoder: Decoder[InfoboxVersion] = Decoder[List[String]].map(InfoboxVersion.apply)

  implicit val ordering: Ordering[InfoboxVersion] =
    Ordering.by[InfoboxVersion, List[String]](_.raw)(Ordering.Implicits.seqOrdering)
}

final case class InfoboxVersion(raw: List[String]) {
  def isSubVersionOf(other: InfoboxVersion): Boolean =
    other.raw.forall(raw.contains)
}
