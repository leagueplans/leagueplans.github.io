package ddm.codec.codecs

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.codec.{BinaryString, Encoding}
import org.scalatest.Assertion

final class CharCodecTest extends CodecSpec {
  "CharCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(c: Char, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(c, Decoder.decodeVarint, expectedEncoding)

      "Char.MinValue" in test(Char.MinValue, Array(0x0))
      "Char.MaxValue" in test(Char.MaxValue, Array(-0x1, -0x1, 0x3))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Char](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _17BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(16)}"))
      Decoder.decode[Char](_17BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
