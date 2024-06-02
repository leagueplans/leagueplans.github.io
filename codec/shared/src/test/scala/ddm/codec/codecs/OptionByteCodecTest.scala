package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.{Decoder, DecodingFailure}
import org.scalatest.Assertion

final class OptionByteCodecTest extends CodecSpec {
  "OptionByteCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(maybeByte: Option[Byte]): Assertion =
        testRoundTripEncoding(maybeByte, Encoding.Len(maybeByte.toArray))

      "None" in test(None)
      "Some(0x0)" in test(Some(0x0))
      "Some(Byte.MinValue)" in test(Some(Byte.MinValue))
      "Some(Byte.MaxValue)" in test(Some(Byte.MaxValue))
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Option[Byte]](_, Decoder.decodeLen))

    "decoding should fail for a multibyte len" in(
      Decoder.decode[Byte](
        Encoding.Len(Array(0x0, 0x1))
      ).left.value shouldBe a[DecodingFailure]
    )
  }
}
