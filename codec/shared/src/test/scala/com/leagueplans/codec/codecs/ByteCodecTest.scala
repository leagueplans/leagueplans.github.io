package com.leagueplans.codec.codecs

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import org.scalatest.Assertion

final class ByteCodecTest extends CodecSpec {
  "ByteCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(byte: Byte): Assertion =
        testRoundTripEncoding(byte, Encoding.Len(Array(byte)))

      "0b0" in test(0b0)

      "0b1" in test(0b1)
      "0b11" in test(0b11)
      "Byte.MaxValue" in test(Byte.MaxValue)

      "-0b0" in test(-0b0)
      "-0b1" in test(-0b1)
      "Byte.MinValue" in test(Byte.MinValue)
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Byte](_, Decoder.decodeLen))

    "decoding should fail for" - {
      def test(len: Encoding.Len): Assertion =
        Decoder.decode[Byte](len).left.value shouldBe a[DecodingFailure]

      "an empty len" in test(Encoding.Len(Array.empty))
      "a multibyte len" in test(Encoding.Len(Array(0b0, 0b1)))
    }
  }
}
