package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.{BinaryString, Encoding}

final class CharCodecTest extends CodecSpec {
  "CharCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "Char.MinValue" in testRoundTripEncoding(
        Char.MinValue,
        Encoding.Varint(BinaryString.unsafe("0"))
      )
      
      "Char.MaxValue" in testRoundTripEncoding(
        Char.MaxValue,
        Encoding.Varint(BinaryString.unsafe("1".repeat(16)))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Char](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _17BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(16)}"))
      Decoder.decode[Char](_17BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
