package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

final class DoubleCodecTest extends CodecSpec {
  "DoubleCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(d: Double): Assertion =
        testRoundTripEncoding(d, Encoding.I64(d))

      "0" in test(0)

      "1" in test(1)
      "2" in test(2)
      "3.14159" in test(3.14159)
      "Double.MaxValue" in test(Double.MaxValue)

      "-1" in test(-1)
      "-2" in test(-2)
      "-0.000001602" in test(-0.000001602)
      "Double.MinValue" in test(Double.MinValue)
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Double](_, Decoder.decodeI64))
  }
}
