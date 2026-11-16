package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object WorldPoint {
  given Encoder[WorldPoint] = Encoder.derived
  given Decoder[WorldPoint] = Decoder.derived
}

/** A tile in the game world, in the game's own coordinates.
  *
  * The surface lies roughly within x 1024–3970 and y 2500–4160. Most dungeons and basements are
  * the surface shifted 6400 north, and minigames and quest areas sit in the band between.
  *
  * @param plane the floor, from 0 (the ground) to 3
  */
final case class WorldPoint(x: Int, y: Int, plane: Int)
