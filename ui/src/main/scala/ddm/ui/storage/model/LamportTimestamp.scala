package ddm.ui.storage.model

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

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
