package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.{Decoder, DecodingFailure}
import org.scalatest.Assertion

final class ByteCodecTest extends CodecSpec {
  "ByteCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(byte: Byte): Assertion =
        testRoundTripEncoding(byte, Encoding.Len(Array(byte)))

      "0x0" in test(0x0)

      "0x1" in test(0x1)
      "0x2" in test(0x2)
      "Byte.MaxValue" in test(Byte.MaxValue)

      "-0x1" in test(-0x1)
      "-0x2" in test(-0x2)
      "Byte.MinValue" in test(Byte.MinValue)
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Byte](_, Decoder.decodeLen))

    "decoding should fail for" - {
      def test(len: Encoding.Len): Assertion =
        Decoder.decode[Byte](len).left.value shouldBe a[DecodingFailure]

      "an empty len" in test(Encoding.Len(Array.empty))
      "a multibyte len" in test(Encoding.Len(Array(0x0, 0x1)))
    }
  }
}
