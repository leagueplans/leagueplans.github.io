package com.leagueplans.ui.storage.model

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalatest.Assertion

final class SchemaVersionTest extends CodecSpec {
  "SchemaVersion" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(version: SchemaVersion, discriminant: Byte): Assertion =
        testRoundTripSerialisation(
          version,
          Decoder.decodeMessage,
          Array(0, discriminant, 0b1100, 0)
        )

      "V1" in test(SchemaVersion.V1, 0)
      "V2" in test(SchemaVersion.V2, 1)
    }
  }
}
