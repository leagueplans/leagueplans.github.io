package ddm.codec.codecs

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.{BinaryString, Encoding}
import org.scalatest.Assertion

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
