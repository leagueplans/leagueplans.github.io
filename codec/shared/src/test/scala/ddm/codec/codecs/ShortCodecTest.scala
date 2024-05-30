package ddm.codec.codecs

import ddm.codec.{BinaryString, Encoding}
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class ShortCodecTest extends CodecSpec {
  "ShortCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(s: Short, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(s, Decoder.decodeVarint, expectedEncoding)

      "0" in test(0, Array(0x0))

      // The short encoder should use zigzag encoding
      "1" in test(1, Array(0x2))
      "2" in test(2, Array(0x4))
      "Short.MaxValue" in test(Short.MaxValue, Array(-0x2, -0x1, 0x3))

      "-1" in test(-1, Array(0x1))
      "-2" in test(-2, Array(0x3))
      "Short.MinValue" in test(Short.MinValue, Array(-0x1, -0x1, 0x3))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Short](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _17BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(16)}"))
      Decoder.decode[Short](_17BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
