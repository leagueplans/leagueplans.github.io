package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

final class ByteArrayCodecTest extends CodecSpec {
  "ByteArrayCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(array: Array[Byte]): Assertion =
        testRoundTripEncoding(array, Encoding.Len(array))

      "Array.empty" in test(Array.empty)
      "Array(0b0)" in test(Array(0b0))
      "Array(Byte.MinValue)" in test(Array(Byte.MinValue))
      "Array(Byte.MaxValue)" in test(Array(Byte.MaxValue))
      "A multibyte array" in test(Array(0x32, -0x24, 0x0, 0x7c, 0x7c, -0x7a))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Array[Byte]](_, Decoder.decodeLen))
  }
}
