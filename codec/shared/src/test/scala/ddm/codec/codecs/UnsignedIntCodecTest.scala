package ddm.codec.codecs

import ddm.codec.{BinaryString, Encoding}
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class UnsignedIntCodecTest extends CodecSpec {
  private given encoder: Encoder[Int] = Encoder.unsignedIntEncoder
  private given decoder: Decoder[Int] = Decoder.unsignedIntDecoder

  "UnsignedIntCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(i: Int, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(i, Decoder.decodeVarint, expectedEncoding)

      "0" in test(0, Array(0x0))

      "1" in test(1, Array(0x1))
      "2" in test(2, Array(0x2))
      "Int.MaxValue" in test(Int.MaxValue, Array(-0x1, -0x1, -0x1, -0x1, 0x7))

      "-1" in test(-1, Array(-0x1, -0x1, -0x1, -0x1, 0xf))
      "-2" in test(-2, Array(-0x2, -0x1, -0x1, -0x1, 0xf))
      "Int.MinValue" in test(Int.MinValue, Array(-0x80, -0x80, -0x80, -0x80, 0x8))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Int](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _33BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(32)}"))
      Decoder.decode[Int](_33BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
