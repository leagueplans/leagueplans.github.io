package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.LeagueTaskTier
import com.leagueplans.ui.model.player.mode.{LeaguesII, Mode}

final class LeaguePointScoringTest extends CodecSpec {
  "LeaguePointScoring" - {
    "encoding values to and decoding values from an expected encoding" in {
      val tier1 = LeagueTaskTier.Easy
      val tier1Qnt = 5

      val tier2 = LeagueTaskTier.Medium
      val tier2Qnt = 20

      testRoundTripSerialisation(
        LeaguePointScoring(
          LeaguesII,
          Map(tier1 -> tier1Qnt, tier2 -> tier2Qnt)
        ),
        Decoder.decodeMessage,
        Array[Byte](0b11, 0b1001) ++ Encoder.encode[Mode.League](LeaguesII).getBytes ++
          Array[Byte](0b1100, 0b1000, 0b100, 0b100) ++ Encoder.encode(tier1).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(tier1Qnt).getBytes ++
          Array[Byte](0b1100, 0b1000, 0b100, 0b100) ++ Encoder.encode(tier2).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(tier2Qnt).getBytes
      )
    }
  }
}
