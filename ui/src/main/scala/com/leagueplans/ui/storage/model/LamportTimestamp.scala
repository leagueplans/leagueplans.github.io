package com.leagueplans.ui.storage.model

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

opaque type LamportTimestamp <: Int = Int

object LamportTimestamp {
  val initial: LamportTimestamp = 0

  extension (self: LamportTimestamp) {
    def increment: LamportTimestamp =
      self + 1
  }

  given Encoder[LamportTimestamp] = Encoder.unsignedIntEncoder
  given Decoder[LamportTimestamp] = Decoder.unsignedIntDecoder
}
