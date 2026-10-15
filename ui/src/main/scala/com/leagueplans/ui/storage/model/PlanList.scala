package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object PlanList {
  given Encoder[PlanList] = Encoder.derived
  given Decoder[PlanList] = Decoder.derived
}

/** The plans in storage
  *
  * @param unreadable plans whose metadata couldn't be read, such as ones saved by a newer version of
  *                   the site. They can't be opened, but their files can still be downloaded.
  */
final case class PlanList(
  readable: Map[PlanID, PlanMetadata],
  unreadable: Set[PlanID]
)
