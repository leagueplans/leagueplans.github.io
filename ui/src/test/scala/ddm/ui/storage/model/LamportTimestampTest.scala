package ddm.ui.storage.model

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

import scala.annotation.tailrec

final class LamportTimestampTest extends CodecSpec {
  "LamportTimestamp" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(n: Int, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation[LamportTimestamp](
          buildTimestamp(LamportTimestamp.initial, n),
          Decoder.decodeVarint,
          expectedEncoding
        )

      @tailrec
      def buildTimestamp(ts: LamportTimestamp, remainder: Int): LamportTimestamp =
        if (remainder > 0)
          buildTimestamp(ts.increment, remainder - 1)
        else
          ts

      "0" in test(0, Array(0))
      "1" in test(1, Array(0b1))
      "2" in test(2, Array(0b10))
      "a large timestamp" in test(10000, Array(-0b1110000, 0b1001110))
    }
  }
}
