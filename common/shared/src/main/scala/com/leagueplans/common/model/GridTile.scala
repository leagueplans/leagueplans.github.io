package com.leagueplans.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

object GridTile {
  given Codec[GridTile] = deriveCodec
  given Ordering[GridTile] = Ordering.by(_.id)
}

final case class GridTile(id: Int, description: String, row: Int, column: Int)
