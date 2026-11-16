package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder

final class WorldPointTest extends CodecSpec {
  "WorldPoint" - {
    "encoding values to and decoding values from an expected encoding" in {
      testRoundTripSerialisation(
        WorldPoint(3222, 3218, 1),
        Decoder.decodeMessage,
        Array[Byte](
          // x: 3222, zigzagged to 6444, as a varint
          0, 0b10101100.toByte, 0b110010,
          // y: 3218, zigzagged to 6436
          0b1000, 0b10100100.toByte, 0b110010,
          // plane: 1, zigzagged to 2
          0b10000, 0b10
        )
      )
    }
  }
}
