package com.leagueplans.common.model

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalatest.Assertion

final class LeagueTaskTierTest extends CodecSpec {
  "LeagueTaskTier" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(tier: LeagueTaskTier, discriminant: Byte): Assertion =
        testRoundTripSerialisation(
          tier,
          Decoder.decodeMessage,
          Array(0, discriminant, 0b1100, 0)
        )

      "Beginner" in test(LeagueTaskTier.Beginner, 0)
      "Easy" in test(LeagueTaskTier.Easy, 0b1)
      "Medium" in test(LeagueTaskTier.Medium, 0b10)
      "Hard" in test(LeagueTaskTier.Hard, 0b11)
      "Elite" in test(LeagueTaskTier.Elite, 0b100)
      "Master" in test(LeagueTaskTier.Master, 0b101)
    }
  }
}
