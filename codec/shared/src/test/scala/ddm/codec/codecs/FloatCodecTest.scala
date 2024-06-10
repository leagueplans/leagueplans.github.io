package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class FloatCodecTest extends CodecSpec {
  "FloatCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(f: Float): Assertion =
        testRoundTripEncoding(f, Encoding.I32(f))

      "0" in test(0)

      "1" in test(1)
      "2" in test(2)
      "3.14159" in test(3.14159)
      "Float.MinPositiveValue" in test(Float.MinPositiveValue)
      "Float.MaxValue" in test(Float.MaxValue)
      "Float.PositiveInfinity" in test(Float.PositiveInfinity)

      "-1" in test(-1)
      "-2" in test(-2)
      "-0.000001602" in test(-0.000001602)
      "Float.MinValue" in test(Float.MinValue)
      "Float.NegativeInfinity" in test(Float.NegativeInfinity)

      "Float.NaN" - {
        "Encoding" in {
          val encoding = Encoder.encode(Float.NaN)
          encoding shouldBe a[Encoding.I32]
          encoding.asInstanceOf[Encoding.I32].value.isNaN shouldEqual true
        }

        "Decoding" in(
          Decoder.decode[Float](Encoding.I32(Float.NaN)).value.isNaN shouldEqual true
        )
      }
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Float](_, Decoder.decodeI32))
  }
}
