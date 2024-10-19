package ddm.ui.storage.model

import ddm.codec.codecs.CodecSpec
import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import org.scalactic.Equality

import scala.scalajs.js.Date

final class PlanMetadataTest extends CodecSpec {
  private given Equality[PlanMetadata] = {
    case (a: PlanMetadata, b: PlanMetadata) =>
      a.name == b.name &&
        a.timestamp.getTime() == b.timestamp.getTime() &&
        a.schemaVersion == b.schemaVersion

    case _ => false
  }

  "PlanMetadata" - {
    "encoding values to and decoding values from an expected encoding" in {
      val name = "My plan"
      val timestamp = new Date(year = 2024, month = 12, date = 10)
      val schemaVersion = SchemaVersion.V1

      testRoundTripSerialisation(
        PlanMetadata(name, timestamp, schemaVersion),
        Decoder.decodeMessage,
        Array[Byte](0b11, 0b111) ++ Encoder.encode(name).getBytes ++
          Array[Byte](0b1001) ++ Encoder.encode(timestamp.getTime()).getBytes ++
          Array[Byte](0b10100, 0b100) ++ Encoder.encode(schemaVersion).getBytes
      )
    }
  }
}
