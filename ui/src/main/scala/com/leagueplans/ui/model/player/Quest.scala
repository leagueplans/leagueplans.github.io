package com.leagueplans.ui.model.player

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object Quest {
  given Codec[Quest] = deriveCodec[Quest]
}

final case class Quest(id: Int, name: String, points: Int)
