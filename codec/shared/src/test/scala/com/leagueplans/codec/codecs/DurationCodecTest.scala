package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.{BinaryString, Encoding, FieldNumber}
import org.scalatest.Assertion

import scala.concurrent.duration.{Duration, FiniteDuration, NANOSECONDS}

final class DurationCodecTest extends CodecSpec {
  "DurationCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(duration: Duration, expectedEncoding: Encoding): Assertion =
        testRoundTripEncoding(duration, expectedEncoding)
      
      "Duration.Zero" in test(
        Duration.Zero,
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
          FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("0")))
        ))
      )

      "1ns" in test(
        FiniteDuration(1L, NANOSECONDS),
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
          FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("1")))
        ))
      )
      
      "Duration.Inf" in test(
        Duration.Inf,
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
          FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("10")))
        ))
      )

      "Duration.MinusInf" in test(
        Duration.MinusInf,
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
          FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("1")))
        ))
      )

      "Duration.Undefined" in test(
        Duration.Undefined,
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
          FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("0")))
        ))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven finite durations" in
      forAll((l: Long) =>
        whenever(l != Long.MinValue)(
          testRoundTripSerialisation[Duration](FiniteDuration(l, NANOSECONDS), Decoder.decodeMessage)
        )
      )

    "decoding should fail for unknown infinite values" in {
      val unknownTag = Encoding.Message(Map(
        FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
        FieldNumber(1) -> List(Encoding.Varint(BinaryString.unsafe("110")))
      ))
      Decoder.decode[Duration](unknownTag).left.value shouldBe a[DecodingFailure]
    }
  }
}
