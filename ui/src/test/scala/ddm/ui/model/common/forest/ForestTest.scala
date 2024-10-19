package ddm.ui.model.common.forest

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalatest.Assertion

final class ForestTest extends CodecSpec {
  "Forest" - {
    val parentID = 42
    val parentIDEnc = Encoder.encode(parentID).getBytes
    val parent = 'p'
    val parentEnc = Encoder.encode(parent).getBytes

    val child1ID = 26
    val child1IDEnc = Encoder.encode(child1ID).getBytes
    val child1 = 'x'
    val child1Enc = Encoder.encode(child1).getBytes

    val child2ID = 17
    val child2IDEnc = Encoder.encode(child2ID).getBytes
    val child2 = 'y'
    val child2Enc = Encoder.encode(child2).getBytes

    "Update" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(update: Forest.Update[Int, Char], expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(update, Decoder.decodeMessage, expectedEncoding)

        "AddNode" in test(
          Forest.Update.AddNode(parentID, parent),
          Array[Byte](0, 0, 0b1100, 0b100, 0) ++ parentIDEnc ++
            Array[Byte](0b1000) ++ parentEnc
        )

        "RemoveNode" in test(
          Forest.Update.RemoveNode(parentID),
          Array[Byte](0, 0b1, 0b1100, 0b10, 0) ++ parentIDEnc
        )

        "AddLink" in test(
          Forest.Update.AddLink(child1ID, parentID),
          Array[Byte](0, 0b10, 0b1100, 0b100, 0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )

        "RemoveLink" in test(
          Forest.Update.RemoveLink(child1ID, parentID),
          Array[Byte](0, 0b11, 0b1100, 0b100, 0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )

        "ChangeParent" in test(
          Forest.Update.ChangeParent(child2ID, parentID, child1ID),
          Array[Byte](0, 0b100, 0b1100, 0b110, 0) ++ child2IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc ++
            Array[Byte](0b10000) ++ child1IDEnc
        )

        "UpdateData" in test(
          Forest.Update.UpdateData(parentID, child1),
          Array[Byte](0, 0b101, 0b1100, 0b100, 0) ++ parentIDEnc ++
            Array[Byte](0b1000) ++ child1Enc
        )

        "Reorder" in test(
          Forest.Update.Reorder(List(child2ID, child1ID), parentID),
          Array[Byte](0, 0b110, 0b1100, 0b110, 0) ++ child2IDEnc ++
            Array[Byte](0) ++ child1IDEnc ++
            Array[Byte](0b1000) ++ parentIDEnc
        )
      }
    }

    "encoding values to and decoding values from an expected encoding" in
      testRoundTripSerialisation(
        Forest.from(
          nodes = Map(parentID -> parent, child1ID -> child1, child2ID -> child2),
          parentsToChildren = Map(parentID -> List(child1ID, child2ID))
        ),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b100, 0) ++ parentIDEnc ++
          Array[Byte](0b1000) ++ parentEnc ++
          Array[Byte](0b100, 0b100, 0) ++ child1IDEnc ++
          Array[Byte](0b1000) ++ child1Enc ++
          Array[Byte](0b100, 0b100, 0) ++ child2IDEnc ++
          Array[Byte](0b1000) ++ child2Enc ++
          Array[Byte](0b1100, 0b110, 0) ++ parentIDEnc ++
          Array[Byte](0b1000) ++ child1IDEnc ++
          Array[Byte](0b1000) ++ child2IDEnc ++
          Array[Byte](0b1100, 0b10, 0) ++ child1IDEnc ++
          Array[Byte](0b1100, 0b10, 0) ++ child2IDEnc
      )
  }
}
