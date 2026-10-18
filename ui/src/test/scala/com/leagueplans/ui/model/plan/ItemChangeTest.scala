package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import org.scalatest.Assertion

final class ItemChangeTest extends CodecSpec {
  "ItemChange" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(change: ItemChange, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(change, Decoder.decodeMessage, expectedEncoding)

      "By, adding" in {
        val amountEnc = Encoder.encode(25).getBytes
        test(
          ItemChange.By(25),
          Array[Byte](0, 0, 0b1100, (1 + amountEnc.length).toByte, 0) ++ amountEnc
        )
      }

      "By, removing" in {
        val amountEnc = Encoder.encode(-25).getBytes
        test(
          ItemChange.By(-25),
          Array[Byte](0, 0, 0b1100, (1 + amountEnc.length).toByte, 0) ++ amountEnc
        )
      }

      "Fill" in test(ItemChange.Fill, Array[Byte](0, 0b1, 0b1100, 0))
      "Empty" in test(ItemChange.Empty, Array[Byte](0, 0b10, 0b1100, 0))
    }
  }
}
