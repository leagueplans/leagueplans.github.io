package ddm.ui.storage.model

import io.circe.{Encoder, Decoder}

opaque type LamportTimestamp <: Int = Int

object LamportTimestamp {
  val initial: LamportTimestamp = 0

  extension (self: LamportTimestamp) {
    def increment: LamportTimestamp =
      self + 1
  }

  given Encoder[LamportTimestamp] = Encoder.encodeInt
  given Decoder[LamportTimestamp] = Decoder.decodeInt
}
