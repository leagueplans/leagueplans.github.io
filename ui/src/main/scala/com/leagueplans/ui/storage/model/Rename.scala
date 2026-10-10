package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

object Rename {
  given Encoder[Rename] = Encoder.derived
  given Decoder[Rename] = Decoder.derived
}

/** An update that changes a plan's name, which is kept apart from the rest of the plan */
final case class Rename(name: String)
