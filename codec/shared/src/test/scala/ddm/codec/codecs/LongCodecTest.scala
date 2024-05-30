package ddm.codec.codecs

import ddm.codec.{BinaryString, Encoding}
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class LongCodecTest extends CodecSpec {
  "LongCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(l: Long, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(l, Decoder.decodeVarint, expectedEncoding)

      "0" in test(0L, Array(0x0))

      // The long encoder should use zigzag encoding
      "1" in test(1L, Array(0x2))
      "2" in test(2L, Array(0x4))
      "Long.MaxValue" in test(Long.MaxValue, Array(-0x2, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, 0x1))

      "-1" in test(-1L, Array(0x1))
      "-2" in test(-2L, Array(0x3))
      "Long.MinValue" in test(Long.MinValue, Array(-0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, -0x1, 0x1))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Long](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _65BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(64)}"))
      Decoder.decode[Long](_65BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
