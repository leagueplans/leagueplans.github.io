package com.leagueplans.ui.storage.model

import com.leagueplans.codec.CharacterEncodings as CE
import com.leagueplans.codec.CharacterEncodings.*
import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalatest.Assertion

final class PlanIDTest extends CodecSpec {
  "PlanID" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(s: String, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation[PlanID](
          PlanID.fromString(s),
          Decoder.decodeLen,
          expectedEncoding
        )

      "a typical string ID" in test(
        "my plan",
        Array(`m`, `y`, ` `, `p`, `l`, CE.`a`, `n`)
      )

      "a typical UUID-based ID" in test(
        "a385ac11-8985-4cd3-a2d4-8e1a567d2a3d",
        Array(
          CE.`a`, `3`, `8`, `5`, CE.`a`, `c`, `1`, `1`, `-`,
          `8`, `9`, `8`, `5`, `-`,
          `4`, `c`, `d`, `3`, `-`,
          CE.`a`, `2`, `d`, `4`, `-`,
          `8`, `e`, `1`, CE.`a`, `5`, `6`, `7`, `d`, `2`, CE.`a`, `3`, `d`
        )
      )
    }
  }
}
