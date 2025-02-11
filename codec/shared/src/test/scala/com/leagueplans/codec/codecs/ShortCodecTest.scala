package com.leagueplans.codec.codecs

import com.leagueplans.codec.{BinaryString, Encoding}
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}

final class ShortCodecTest extends CodecSpec {
  "ShortCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "0" in testRoundTripEncoding(0, Encoding.Varint(BinaryString.unsafe("0")))

      // The short encoder should use zigzag encoding
      "1" in testRoundTripEncoding(1, Encoding.Varint(BinaryString.unsafe("10")))
      "2" in testRoundTripEncoding(2, Encoding.Varint(BinaryString.unsafe("100")))
      "Short.MaxValue" in testRoundTripEncoding(
        Short.MaxValue,
        Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(15)}0"))
      )

      "-1" in testRoundTripEncoding(-1, Encoding.Varint(BinaryString.unsafe("1")))
      "-2" in testRoundTripEncoding(-2, Encoding.Varint(BinaryString.unsafe("11")))
      "Short.MinValue" in testRoundTripEncoding(
        Short.MinValue,
        Encoding.Varint(BinaryString.unsafe("1".repeat(16)))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Short](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _17BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(16)}"))
      Decoder.decode[Short](_17BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
