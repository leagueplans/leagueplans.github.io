package com.leagueplans.codec.codecs

import com.leagueplans.codec.{BinaryString, Encoding}
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}

final class IntCodecTest extends CodecSpec {
  "IntCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "0" in testRoundTripEncoding(0, Encoding.Varint(BinaryString.unsafe("0")))

      // The default int encoder should use zigzag encoding
      "1" in testRoundTripEncoding(1, Encoding.Varint(BinaryString.unsafe("10")))
      "2" in testRoundTripEncoding(2, Encoding.Varint(BinaryString.unsafe("100")))
      "Int.MaxValue" in testRoundTripEncoding(
        Int.MaxValue,
        Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(31)}0"))
      )

      "-1" in testRoundTripEncoding(-1, Encoding.Varint(BinaryString.unsafe("1")))
      "-2" in testRoundTripEncoding(-2, Encoding.Varint(BinaryString.unsafe("11")))
      "Int.MinValue" in testRoundTripEncoding(
        Int.MinValue, 
        Encoding.Varint(BinaryString.unsafe("1".repeat(32)))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Int](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _33BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(32)}"))
      Decoder.decode[Int](_33BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
