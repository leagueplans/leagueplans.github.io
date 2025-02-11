package com.leagueplans.ui.storage.model

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

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
