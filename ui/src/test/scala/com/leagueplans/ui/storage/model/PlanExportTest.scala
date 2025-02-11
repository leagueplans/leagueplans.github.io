package com.leagueplans.ui.storage.model

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.codec.{BinaryString, Encoding, FieldNumber, MapEquality}
import com.leagueplans.ui.model.plan.Step
import org.scalactic.Equality

final class PlanExportTest extends CodecSpec {
  private val stepsEquality = MapEquality[Step.ID, Encoding](using encodingEquality)

  private given Equality[PlanExport] = {
    case (a: PlanExport, b: PlanExport) =>
      encodingEquality.areEqual(a.metadata, b.metadata) &&
        encodingEquality.areEqual(a.settings, b.settings) &&
        encodingEquality.areEqual(a.mappings, b.mappings) &&
        stepsEquality.areEqual(a.steps, b.steps)

    case (_, _) => false
  }

  "PlanExport" - {
    "encoding values to and decoding values from an expected encoding" in {
      val metadata = Encoding.I32(3.2f)
      val settings = Encoding.Len(Array(0b10110, 0b101))
      val mappings = Encoding.I64(56.7)
      val stepID = Step.ID.fromString("id")
      val stepEncoding = Encoding.Message(Map(FieldNumber(2) -> List(Encoding.Varint(BinaryString(11)))))

      testRoundTripSerialisation(
        PlanExport(metadata, settings, mappings, Map(stepID -> stepEncoding)),
        Decoder.decodeMessage,
        Array[Byte](0b10) ++ metadata.getBytes ++
          Array[Byte](0b1011, 0b10) ++ settings.getBytes ++
          Array[Byte](0b10001) ++ mappings.getBytes ++
          Array[Byte](0b11100, 0b1000, 0b11, 0b10) ++ Encoder.encode(stepID).getBytes ++
          Array[Byte](0b1100, 0b10) ++ stepEncoding.getBytes
      )
    }
  }
}
