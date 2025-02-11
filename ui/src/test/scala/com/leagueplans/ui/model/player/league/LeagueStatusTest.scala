package com.leagueplans.ui.model.player.league

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill

final class LeagueStatusTest extends CodecSpec {
  "LeagueStatus" - {
    "encoding values to and decoding values from an expected encoding" in {
      val leaguePoints = 10
      val completedTask = 57

      testRoundTripSerialisation(
        LeagueStatus(leaguePoints, Set(completedTask), Set(Skill.Herblore)),
        Decoder.decodeMessage,
        Array[Byte](0) ++ Encoder.encode(leaguePoints).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(completedTask).getBytes ++
          Array[Byte](0b10100, 0b100) ++ Encoder.encode(Skill.Herblore).getBytes
      )
    }
  }
}
