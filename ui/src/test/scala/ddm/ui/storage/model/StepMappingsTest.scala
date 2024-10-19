package ddm.ui.storage.model

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.ui.model.plan.Step

final class StepMappingsTest extends CodecSpec {
  "StepMappings" - {
    "encoding values to and decoding values from an expected encoding" in {
      val id1 = Step.ID.fromString("a")
      val id2 = Step.ID.fromString("b")
      val id3 = Step.ID.fromString("c")

      testRoundTripSerialisation(
        StepMappings(Map(id1 -> List(id2, id3))),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b1001, 0b11, 0b1) ++ Encoder.encode(id1).getBytes ++
          Array[Byte](0b1011, 0b1) ++ Encoder.encode(id2).getBytes ++
          Array[Byte](0b1011, 0b1) ++ Encoder.encode(id3).getBytes
      )
    }
  }
}
