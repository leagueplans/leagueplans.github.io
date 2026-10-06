package com.leagueplans.ui.model.player

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.{BankSpace, Depository}
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
      val bankSpace = BankSpace(Set(BankSpace.Unlock.Pin), blocksBought = 2)

      testRoundTripSerialisation(
        Player(stats, Map(bank.kind -> bank), Set(completedQuest), Set(completedDiaryTask), leagueStatus, gridStatus, bankSpace),
        Decoder.decodeMessage,
        // This doesn't match the ordering of the fields, but that's fine. It's because we use maps from
        // field number to field encoding to represent the data.
        Array[Byte](0b100, 0b1001, 0b100, 0b100) ++ Encoder.encode(Skill.Woodcutting).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(exp).getBytes ++
          Array[Byte](0b101100, 0b11) ++ Encoder.encode(gridStatus).getBytes ++
          Array[Byte](0b1100, 0b10010) ++ Encoder.encode(bank).getBytes ++
          Array[Byte](0b110100, 0b1000) ++ Encoder.encode(bankSpace).getBytes ++
          Array[Byte](0b10000) ++ Encoder.encode(completedQuest).getBytes ++
          Array[Byte](0b11000) ++ Encoder.encode(completedDiaryTask).getBytes ++
          Array[Byte](0b100100, 0b1011) ++ Encoder.encode(leagueStatus).getBytes
      )
    }

    "decoding a player encoded before there was bank space, as having nothing unlocked" in {
      val player = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))
      val bytes = Encoder.encode(player).getBytes.toList
      // Field 6, two bytes long, holding a bank space with nothing unlocked
      val bankSpaceField = List[Byte](0b110100, 0b10, 0b1000, 0)
      bytes.containsSlice(bankSpaceField) shouldBe true

      val withoutBankSpace = bytes.patch(bytes.indexOfSlice(bankSpaceField), Nil, bankSpaceField.size)
      Decoder.decodeMessage[Player](withoutBankSpace.toArray) shouldBe Right(player)
    }

    "has the bank's capacity from its bank space" in {
      val player = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))
      player.capacity(Depository.Kind.Bank) shouldBe 900
      player.copy(bankSpace = BankSpace(Set(BankSpace.Unlock.Pin), blocksBought = 1)).capacity(Depository.Kind.Bank) shouldBe 970
      player.capacity(Depository.Kind.Inventory) shouldBe 28
    }
  }
}
