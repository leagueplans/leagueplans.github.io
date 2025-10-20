package com.leagueplans.ui.model.player

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Stats}

final class PlayerTest extends CodecSpec {
  "Player" - {
    "encoding values to and decoding values from an expected encoding" in {
      val exp = Exp(175)
      val stats = Stats(Skill.Woodcutting -> exp)
      val bank = Depository(Map((Item.ID(23451), true) -> 25), Depository.Kind.Bank)
      val completedQuest = 2
      val completedDiaryTask = 64
      val leagueStatus = LeagueStatus(
        leaguePoints = 5,
        completedTasks = Set(123),
        skillsUnlocked = Set(Skill.Woodcutting)
      )
      val gridStatus = GridStatus(completedTiles = Set(92))

      testRoundTripSerialisation(
        Player(stats, Map(bank.kind -> bank), Set(completedQuest), Set(completedDiaryTask), leagueStatus, gridStatus),
        Decoder.decodeMessage,
        // This doesn't match the ordering of the fields, but that's fine. It's because we use maps from
        // field number to field encoding to represent the data.
        Array[Byte](0b100, 0b1001, 0b100, 0b100) ++ Encoder.encode(Skill.Woodcutting).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(exp).getBytes ++
          Array[Byte](0b101100, 0b11) ++ Encoder.encode(gridStatus).getBytes ++
          Array[Byte](0b1100, 0b10010) ++ Encoder.encode(bank).getBytes ++
          Array[Byte](0b10000) ++ Encoder.encode(completedQuest).getBytes ++
          Array[Byte](0b11000) ++ Encoder.encode(completedDiaryTask).getBytes ++
          Array[Byte](0b100100, 0b1011) ++ Encoder.encode(leagueStatus).getBytes
      )
    }
  }
}
