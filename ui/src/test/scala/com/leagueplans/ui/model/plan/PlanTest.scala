package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, LeagueTaskTier, Skill}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.mode.{LeaguesIII, MainGame, Mode}
import com.leagueplans.ui.model.player.skill.{Exp, Stats}

final class PlanTest extends CodecSpec {
  "Plan" - {
    val player = Player(
      Stats(Skill.Woodcutting -> Exp(175)),
      Map(Depository.Kind.Bank -> Depository(Map.empty, Depository.Kind.Bank)),
      completedQuests = Set(2),
      completedDiaryTasks = Set(64),
      LeagueStatus(
        leaguePoints = 5,
        completedTasks = Set(123),
        skillsUnlocked = Set(Skill.Woodcutting)
      )
    )
    val expMultiplier = ExpMultiplier(Skill.values.toSet, base = 10, thresholds = List.empty)
    val leaguePointScoring = LeaguePointScoring(LeaguesIII, Map(LeagueTaskTier.Easy -> 5))
    val deferredSettings = Plan.Settings.Deferred(MainGame)

    "Settings" - {
      "encoding values to and decoding values from an expected encoding" - {
        "Deferred" in testRoundTripSerialisation(
          deferredSettings,
          Decoder.decodeMessage,
          Array[Byte](0, 0, 0b1100, 0b1011, 0b11, 0b1001) ++ Encoder.encode[Mode](MainGame).getBytes
        )

        "Explicit" in testRoundTripSerialisation(
          Plan.Settings.Explicit(player, List(expMultiplier), Some(leaguePointScoring)),
          Decoder.decodeMessage,
          Array[Byte](0, 0b1, 0b1100, -0b110011, 0b1, 0b100, 0b100101) ++ Encoder.encode(player).getBytes ++
            Array[Byte](0b1100, -0b1110100, 0b1) ++ Encoder.encode(expMultiplier).getBytes ++
            Array[Byte](0b10100, 0b10101) ++ Encoder.encode(leaguePointScoring).getBytes
        )
      }
    }

    "encoding values to and decoding values from an expected encoding" in {
      val name = "Deadman: Armageddon plan"

      val step = Step(
        Step.ID.fromString("id"),
        StepDetails(
          description = "Chop a tree",
          directEffects = EffectList(List(Effect.GainExp(Skill.Woodcutting, Exp(25)))),
          requirements = List(Requirement.Tool(Item.ID(241), Depository.Kind.EquipmentSlot.Weapon))
        )
      )

      val forest = Forest.from(
        nodes = Map(step.id -> step),
        parentsToChildren = Map(step.id -> List.empty),
        roots = List(step.id)
      )

      testRoundTripSerialisation(
        Plan(name, forest, deferredSettings),
        Decoder.decodeMessage,
        Array[Byte](0b11, 0b11000) ++ Encoder.encode(name).getBytes ++
          Array[Byte](0b1100, 0b1000111) ++ Encoder.encode(forest).getBytes ++
          Array[Byte](0b10100, 0b1111) ++ Encoder.encode[Plan.Settings](deferredSettings).getBytes
      )
    }
  }
}
