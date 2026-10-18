package com.leagueplans.ui.model.player

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

final class GridStatusTest extends CodecSpec {
  "GridStatus" - {
    "encoding values to and decoding values from an expected encoding" - {
      "no completed tiles" in testRoundTripSerialisation(
        GridStatus(Set.empty),
        Decoder.decodeMessage,
        Array.empty[Byte]
      )

      "completed tiles" in testRoundTripSerialisation(
        GridStatus(Set(3, 62)),
        Decoder.decodeMessage,
        Array[Byte](0) ++ Encoder.encode(3).getBytes ++
          Array[Byte](0) ++ Encoder.encode(62).getBytes
      )
    }
  }
}
