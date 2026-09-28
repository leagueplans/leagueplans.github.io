package com.leagueplans.common.model

import io.circe.{Decoder, Encoder}

object InfoboxKey {
  given Decoder[InfoboxKey] =
    Decoder[(Int, List[String])].map(InfoboxKey.apply)

  given Encoder[InfoboxKey] =
    Encoder[(Int, List[String])].contramap(key => (key.pageID, key.version))

  given Ordering[InfoboxKey] = {
    import Ordering.Implicits.seqOrdering
    Ordering.by(key => (key.pageID, key.version))
  }
}

/** Identifies one infobox on the wiki.
  *
  * A single wiki page can hold several versions of an infobox — one per variant of an
  * item, for example — so the page ID alone is not enough to identify one. The version is
  * empty for pages with exactly one.
  */
final case class InfoboxKey(pageID: Int, version: List[String])
