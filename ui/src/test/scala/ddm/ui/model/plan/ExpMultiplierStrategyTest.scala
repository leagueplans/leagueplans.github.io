package ddm.ui.model.plan

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class ExpMultiplierStrategyTest extends CodecSpec {
  "ExpMultiplierStrategy" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(ems: ExpMultiplierStrategy, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(ems, Decoder.decodeMessage, expectedEncoding)

      "Fixed" in test(
        ExpMultiplierStrategy.Fixed(10),
        Array[Byte](0, 0, 0b1100, 0b10, 0) ++ Encoder.encode(10).getBytes
      )

      "LeaguePointBased" in test(
        ExpMultiplierStrategy.LeaguePointBased(base = 5, thresholds = List((500, 10))),
        Array[Byte](0, 0b1, 0b1100, 0b1001, 0) ++ Encoder.encode(5).getBytes ++
          Array[Byte](0b1100, 0b101, 0) ++ Encoder.encode(500).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(10).getBytes
      )
    }
  }
}
