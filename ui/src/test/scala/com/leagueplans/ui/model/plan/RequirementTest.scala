package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.Level
import org.scalatest.Assertion

final class RequirementTest extends CodecSpec {
  "Requirement" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(req: Requirement, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(req, Decoder.decodeMessage, expectedEncoding)

      val skillLevel = Requirement.SkillLevel(Skill.Fishing, Level.L70)
      val skillLevelEnc = Encoder.encode(skillLevel).getBytes
      val itemID = Item.ID(2352)
      val tool = Requirement.Tool(itemID, Depository.Kind.Inventory)
      val toolEnc = Encoder.encode(tool).getBytes

      "SkillLevel" in test(
        skillLevel,
        Array[Byte](0, 0, 0b1100, 0b1100, 0b100, 0b100) ++ Encoder.encode(Skill.Fishing).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode(Level.L70).getBytes
      )

      "Tool" in test(
        tool,
        Array[Byte](0, 0b1, 0b1100, 0b1001, 0) ++ Encoder.encode(itemID).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode[Depository.Kind](Depository.Kind.Inventory).getBytes
      )

      "And" in test(
        Requirement.And(skillLevel, tool),
        Array[Byte](0, 0b10, 0b1100, 0b100001, 0b100, 0b10000) ++ skillLevelEnc ++
          Array[Byte](0b1100, 0b1101) ++ toolEnc
      )

      "Or" in test(
        Requirement.Or(skillLevel, tool),
        Array[Byte](0, 0b11, 0b1100, 0b100001, 0b100, 0b10000) ++ skillLevelEnc ++
          Array[Byte](0b1100, 0b1101) ++ toolEnc
      )
    }

    "addTo" - {
      val agility50 = Requirement.SkillLevel(Skill.Agility, Level(50))
      val axe = Requirement.Tool(Item.ID(1351), Depository.Kind.Inventory)

      "appends a new requirement" in {
        Requirement.addTo(List(axe), agility50) shouldBe List(axe, agility50)
      }

      "doesn't add a requirement twice" in {
        Requirement.addTo(List(axe, agility50), axe) shouldBe List(axe, agility50)
      }

      "keeps the higher level when a skill is already required" in {
        val agility30 = Requirement.SkillLevel(Skill.Agility, Level(30))
        Requirement.addTo(List(agility30, axe), agility50) shouldBe List(agility50, axe)
        Requirement.addTo(List(agility50, axe), agility30) shouldBe List(agility50, axe)
      }
    }
  }
}
