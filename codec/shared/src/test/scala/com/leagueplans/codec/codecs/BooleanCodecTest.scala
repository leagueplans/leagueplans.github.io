package com.leagueplans.codec.codecs

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.{BinaryString, Encoding}

final class BooleanCodecTest extends CodecSpec {
  "BooleanCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "true" in testRoundTripEncoding(true, Encoding.Varint(BinaryString.unsafe("1")))
      "false" in testRoundTripEncoding(false, Encoding.Varint(BinaryString.unsafe("0")))
    }

    "decoding should fail for varints outside of the encoding range" in {
      val _2BitVarint = Encoding.Varint(BinaryString.unsafe("10"))
      Decoder.decode[Boolean](_2BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
