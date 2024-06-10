package ddm.codec.codecs

import ddm.codec.{BinaryString, Encoding}
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder

final class UnsignedIntCodecTest extends CodecSpec {
  private given encoder: Encoder[Int] = Encoder.unsignedIntEncoder
  private given decoder: Decoder[Int] = Decoder.unsignedIntDecoder

  "UnsignedIntCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      "0" in testRoundTripEncoding(0, Encoding.Varint(BinaryString.unsafe("0")))

      "1" in testRoundTripEncoding(1, Encoding.Varint(BinaryString.unsafe("1")))
      "2" in testRoundTripEncoding(2, Encoding.Varint(BinaryString.unsafe("10")))
      "Int.MaxValue" in testRoundTripEncoding(
        Int.MaxValue, 
        Encoding.Varint(BinaryString.unsafe("1".repeat(31)))
      )

      "-1" in testRoundTripEncoding(-1, Encoding.Varint(BinaryString.unsafe("1".repeat(32))))
      "-2" in testRoundTripEncoding(-2, Encoding.Varint(BinaryString.unsafe(s"${"1".repeat(31)}0")))
      "Int.MinValue" in testRoundTripEncoding(
        Int.MinValue, 
        Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(31)}"))
      )
    }

    "should receive back the same values after round-trip serialisation for generator-driven values" in
      forAll(testRoundTripSerialisation[Int](_, Decoder.decodeVarint))

    "decoding should fail for varints outside of the encoding range" in {
      val _33BitVarint = Encoding.Varint(BinaryString.unsafe(s"1${"0".repeat(32)}"))
      Decoder.decode[Int](_33BitVarint).left.value shouldBe a[DecodingFailure]
    }
  }
}
