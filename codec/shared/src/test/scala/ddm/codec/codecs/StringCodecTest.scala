package ddm.codec.codecs

import ddm.codec.Encoding
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class StringCodecTest extends CodecSpec {
  "StringCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(s: String, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(s, Decoder.decodeLen, expectedEncoding)
      
      "The empty string" in test("", Array.empty)
      
      "Latin characters" in test(
        "Lorem ipsum dolor sit amet.", 
        Array(
          0x4c, 0x6f, 0x72, 0x65, 0x6d, 0x20, 0x69, 0x70, 0x73,
          0x75, 0x6d, 0x20, 0x64, 0x6f, 0x6c, 0x6f, 0x72, 0x20,
          0x73, 0x69, 0x74, 0x20, 0x61, 0x6d, 0x65, 0x74, 0x2e
        )
      )
      
      "Chinese characters" in test(
        "滚滚长江东逝水", 
        Array(
          -0x1a, -0x45, -0x66, -0x1a, -0x45, -0x66, -0x17,
          -0x6b, -0x41, -0x1a, -0x4f, -0x61, -0x1c, -0x48,
          -0x64, -0x17, -0x80, -0x63, -0x1a, -0x50, -0x4c
        )
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[String](_, Decoder.decodeLen))

    "decoding should fail for an illegal UTF-8 byte sequence" in(
      Decoder.decode[String](
        // Bit pattern 1000 0000
        Encoding.Len(Array(-0x1))
      ).left.value shouldBe a[DecodingFailure]
    )
  }
}
