package com.leagueplans.ui.model.player

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object GridStatus {
  given Encoder[GridStatus] = Encoder.derived
  given Decoder[GridStatus] = Decoder.derived
}

final case class GridStatus(completedTiles: Set[Int])
