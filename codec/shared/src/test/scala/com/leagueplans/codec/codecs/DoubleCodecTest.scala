package com.leagueplans.codec.codecs

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
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
      "Double.MinPositiveValue" in test(Double.MinPositiveValue)
      "Double.MaxValue" in test(Double.MaxValue)
      "Double.PositiveInfinity" in test(Double.PositiveInfinity)

      "-1" in test(-1)
      "-2" in test(-2)
      "-0.000001602" in test(-0.000001602)
      "Double.MinValue" in test(Double.MinValue)
      "Double.NegativeInfinity" in test(Double.NegativeInfinity)

      "Double.NaN" - {
        "Encoding" in {
          val encoding = Encoder.encode(Double.NaN)
          encoding shouldBe a[Encoding.I64]
          encoding.asInstanceOf[Encoding.I64].value.isNaN shouldEqual true
        }

        "Decoding" in(
          Decoder.decode[Double](Encoding.I64(Double.NaN)).value.isNaN shouldEqual true
        )
      }
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Double](_, Decoder.decodeI64))
  }
}
