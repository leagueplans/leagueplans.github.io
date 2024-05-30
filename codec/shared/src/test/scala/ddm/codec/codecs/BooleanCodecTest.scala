package ddm.codec.codecs

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.codec.{BinaryString, Encoding}
import org.scalatest.Assertion

final class BooleanCodecTest extends CodecSpec {
  "BooleanCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(bool: Boolean, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(bool, Decoder.decodeVarint, expectedEncoding)

      "true" in test(true, Array(0x1))
      "false" in test(false, Array(0x0))
    }

    "decoding should fail for varints outside of the encoding range" in {
      val _2BitVarint = Encoding.Varint(BinaryString.unsafe("10"))
      Decoder.decode[Boolean](_2BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
