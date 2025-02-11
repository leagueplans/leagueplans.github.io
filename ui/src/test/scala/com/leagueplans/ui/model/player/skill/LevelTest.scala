package com.leagueplans.ui.model.player.skill

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalatest.Assertion

final class LevelTest extends CodecSpec {
  "Level" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(level: Level, discriminant: Byte): Assertion =
        testRoundTripSerialisation(
          level,
          Decoder.decodeMessage,
          Array(0, discriminant, 0b1100, 0)
        )

      Level.values.zip(1 to 99).foreach((level, i) =>
        i.toString in test(level, (i - 1).toByte)
      )
    }
  }
}
