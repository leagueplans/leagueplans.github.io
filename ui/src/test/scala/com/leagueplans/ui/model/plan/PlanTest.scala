package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, LeagueTaskTier, Skill}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.mode.LeaguesIII
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
    val expMultiplierStrategy = ExpMultiplierStrategy.Fixed(10)
    val leaguePointScoring = LeaguePointScoring(LeaguesIII, Map(LeagueTaskTier.Easy -> 5))
    val settings = Plan.Settings(player, expMultiplierStrategy, Some(leaguePointScoring))

    "Settings" - {
      "encoding values to and decoding values from an expected encoding" in
        testRoundTripSerialisation(
          settings,
          Decoder.decodeMessage,
          Array[Byte](0b100, 0b100101) ++ Encoder.encode(player).getBytes ++
            Array[Byte](0b1100, 0b110) ++ Encoder.encode[ExpMultiplierStrategy](expMultiplierStrategy).getBytes ++
            Array[Byte](0b10100, 0b10101) ++ Encoder.encode(leaguePointScoring).getBytes
        )
    }

    "encoding values to and decoding values from an expected encoding" in {
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
        parentsToChildren = Map(step.id -> List.empty)
      )

      testRoundTripSerialisation(
        Plan(forest, settings),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b1000011) ++ Encoder.encode(forest).getBytes ++
          Array[Byte](0b1100, 0b1000110) ++ Encoder.encode(settings).getBytes
      )
    }
  }
}
