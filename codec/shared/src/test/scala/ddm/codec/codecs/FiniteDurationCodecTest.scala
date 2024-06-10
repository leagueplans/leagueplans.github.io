package ddm.codec.codecs

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.{BinaryString, Encoding}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

final class FiniteDurationCodecTest extends CodecSpec {
  "FiniteDurationCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "0ns" in testRoundTripEncoding(
        FiniteDuration(0L, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe("0"))
      )

      // The long encoder should use zigzag encoding
      "1ns" in testRoundTripEncoding(
        FiniteDuration(1L, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe("1"))
      )

      "2ns" in testRoundTripEncoding(
        FiniteDuration(2L, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe("10"))
      )

      "(Long.MaxValue)ns" in testRoundTripEncoding(
        FiniteDuration(Long.MaxValue, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(63)}"))
      )

      "-1ns" in testRoundTripEncoding(
        FiniteDuration(-1L, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe("1".repeat(64)))
      )

      "-2ns" in testRoundTripEncoding(
        FiniteDuration(-2L, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(63)}0"))
      )

      "(Long.MinValue + 1)ns" in testRoundTripEncoding(
        FiniteDuration(Long.MinValue + 1, NANOSECONDS),
        Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(62)}1"))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll((l: Long) =>
        whenever(l != Long.MinValue)(
          testRoundTripSerialisation(FiniteDuration(l, NANOSECONDS), Decoder.decodeVarint)
        )
      )

    "decoding should fail for varints outside of the encoding range" in {
      val longMinValue = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(63)}"))
      Decoder.decode[FiniteDuration](longMinValue).left.value shouldBe a[DecodingFailure]
    }
  }
}
