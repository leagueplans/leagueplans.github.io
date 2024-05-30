package ddm.codec.codecs

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class FloatCodecTest extends CodecSpec {
  "FloatCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(f: Float, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(f, Decoder.decodeI32, expectedEncoding)

      "0" in test(0, Array(0x0, 0x0, 0x0, 0x0))

      // Little endian encoding
      "1" in test(1, Array(0x0, 0x0, -0x80, 0x3f))
      "2" in test(2, Array(0x0, 0x0, 0x0, 0x40))
      "3.14159" in test(3.14159, Array(-0x30, 0xf, 0x49, 0x40))
      "Float.MaxValue" in test(Float.MaxValue, Array(-0x1, -0x1, 0x7f, 0x7f))

      "-1" in test(-1, Array(0x0, 0x0, -0x80, -0x41))
      "-2" in test(-2, Array(0x0, 0x0, 0x0, -0x40))
      "-0.000001602" in test(-0.000001602, Array(0x4d, 0x4, -0x29, -0x4b))
      "Float.MinValue" in test(Float.MinValue, Array(-0x1, -0x1, 0x7f, -0x1))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Float](_, Decoder.decodeI32))
  }
}
