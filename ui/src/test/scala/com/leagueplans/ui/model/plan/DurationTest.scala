package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import org.scalatest.Assertion

final class DurationTest extends CodecSpec {
  "Duration" - {
    "Unit" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(direction: Duration.Unit, discriminant: Byte): Assertion =
          testRoundTripSerialisation(
            direction,
            Decoder.decodeMessage,
            Array(0, discriminant, 0b1100, 0)
          )

        "Ticks" in test(Duration.Unit.Ticks, 0)
        "Seconds" in test(Duration.Unit.Seconds, 0b1)
      }
    }

    "encoding values to and decoding values from an expected encoding" in {
      testRoundTripSerialisation(
        Duration(56, Duration.Unit.Seconds),
        Decoder.decodeMessage,
        Array[Byte](0) ++ Encoder.encode(56).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode(Duration.Unit.Seconds).getBytes
      )
    }
  }
}
