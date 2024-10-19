package ddm.codec.codecs

import ddm.codec.encoding.Encoder
import ddm.codec.{BinaryString, Encoding, FieldNumber}
import org.scalatest.Assertion

final class EitherCodecTest extends CodecSpec {
  "EitherCodec" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(either: Either[Byte, Double], expectedEncoding: Encoding): Assertion =
        testRoundTripEncoding(either, expectedEncoding)

      "Left" in test(
        Left(0b1001011),
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("0"))),
          FieldNumber(1) -> List(Encoder.encode(0b1001011.toByte))
        ))
      )

      "Right" in test(
        Right(23572.13),
        Encoding.Message(Map(
          FieldNumber(0) -> List(Encoding.Varint(BinaryString.unsafe("1"))),
          FieldNumber(1) -> List(Encoder.encode(23572.13))
        ))
      )
    }
  }
}
