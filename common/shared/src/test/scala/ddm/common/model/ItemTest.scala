package ddm.common.model

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import org.scalatest.Assertion

final class ItemTest extends CodecSpec {
  "Item" - {
    "ID" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(id: Int, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation[Item.ID](
            Item.ID(id),
            Decoder.decodeVarint,
            expectedEncoding
          )

        "ID(0)" in test(0, Array(0))
        "ID(1)" in test(1, Array(0b1))
        "ID(5234)" in test(5234, Array(-0b1110, 0b101000))
        "ID(175320)" in test(175320, Array(-0b101000, -0b100111, 0b1010))
      }
    }
  }
}
