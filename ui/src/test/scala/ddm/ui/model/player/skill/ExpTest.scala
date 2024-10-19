package ddm.ui.model.player.skill

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

final class ExpTest extends CodecSpec {
  "Exp" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(exp: Exp, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(exp, Decoder.decodeVarint, expectedEncoding)

      "0 XP" in test(Exp(0), Array(0))
      "XP with a decimal factor" in test(Exp(4.7), Array(0b1011110))
      "Max XP" in test(Exp(200000000), Array(-0b10000000, -0b110000, -0b1010100, -0b1101, 0b1110))
    }
  }
}
