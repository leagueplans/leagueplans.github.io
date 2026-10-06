package com.leagueplans.ui.model.plan

import cats.data.NonEmptyList
import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{EquipmentType, InfoboxKey, Item, Skill}
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
      val holds = Requirement.Holds(itemID, Requirement.Where.InventoryOrEquipped)
      val holdsEnc = Encoder.encode(holds).getBytes

      "SkillLevel" in test(
        skillLevel,
        Array[Byte](0, 0, 0b1100, 0b1100, 0b100, 0b100) ++ Encoder.encode(Skill.Fishing).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode(Level.L70).getBytes
      )

      "Holds" in test(
        holds,
        Array[Byte](0, 0b1, 0b1100, 0b1001, 0) ++ Encoder.encode(itemID).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode(Requirement.Where.InventoryOrEquipped).getBytes
      )

      "And" in test(
        Requirement.And(skillLevel, holds),
        Array[Byte](0, 0b10, 0b1100, 0b100001, 0b100, 0b10000) ++ skillLevelEnc ++
          Array[Byte](0b1100, 0b1101) ++ holdsEnc
      )

      "Or" in test(
        Requirement.Or(skillLevel, holds),
        Array[Byte](0, 0b11, 0b1100, 0b100001, 0b100, 0b10000) ++ skillLevelEnc ++
          Array[Byte](0b1100, 0b1101) ++ holdsEnc
      )
    }

    "held" - {
      def item(equipmentType: Option[EquipmentType]): Item =
        Item(
          Item.ID(1),
          gameID = None,
          "Tool",
          examine = "",
          NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
          Item.Bankable.Yes(stacks = true),
          stackable = false,
          noteable = false,
          equipmentType,
          infobox = InfoboxKey(1, List.empty)
        )

      "needs an item that can't be equipped in the inventory" in {
        Requirement.held(item(None)) shouldBe Requirement.Holds(Item.ID(1), Requirement.Where.Inventory)
      }

      "accepts an equippable item in the inventory or equipped" in {
        Requirement.held(item(Some(EquipmentType.TwoHanded))) shouldBe
          Requirement.Holds(Item.ID(1), Requirement.Where.InventoryOrEquipped)
      }
    }

    "addTo" - {
      val agility50 = Requirement.SkillLevel(Skill.Agility, Level(50))
      val axe = Requirement.Holds(Item.ID(1351), Requirement.Where.Inventory)

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
