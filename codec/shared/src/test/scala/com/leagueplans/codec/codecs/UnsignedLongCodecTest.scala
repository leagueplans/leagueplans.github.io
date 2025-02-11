package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.{BinaryString, Encoding}

final class UnsignedLongCodecTest extends CodecSpec {
  private given encoder: Encoder[Long] = Encoder.unsignedLongEncoder
  private given decoder: Decoder[Long] = Decoder.unsignedLongDecoder
  
  "UnsignedLongCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "0" in testRoundTripEncoding(0L, Encoding.Varint(BinaryString.unsafe("0")))

      "1" in testRoundTripEncoding(1L, Encoding.Varint(BinaryString.unsafe("1")))
      "2" in testRoundTripEncoding(2L, Encoding.Varint(BinaryString.unsafe("10")))
      "Long.MaxValue" in testRoundTripEncoding(
        Long.MaxValue,
        Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(63)}"))
      )

      "-1" in testRoundTripEncoding(-1L, Encoding.Varint(BinaryString.unsafe("1".repeat(64))))
      "-2" in testRoundTripEncoding(-2L, Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(63)}0")))
      "Long.MinValue" in testRoundTripEncoding(
        Long.MinValue,
        Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(63)}"))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Long](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _65BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(64)}"))
      Decoder.decode[Long](_65BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
