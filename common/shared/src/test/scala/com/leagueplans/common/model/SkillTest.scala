package com.leagueplans.common.model

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import org.scalatest.Assertion

final class SkillTest extends CodecSpec {
  "Skill" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(skill: Skill, discriminant: Byte): Assertion =
        testRoundTripSerialisation(
          skill,
          Decoder.decodeMessage,
          Array(0, discriminant, 0b1100, 0)
        )

      "Agility" in test(Skill.Agility, 0)
      "Attack" in test(Skill.Attack, 0b1)
      "Construction" in test(Skill.Construction, 0b10)
      "Cooking" in test(Skill.Cooking, 0b11)
      "Crafting" in test(Skill.Crafting, 0b100)
      "Defence" in test(Skill.Defence, 0b101)
      "Farming" in test(Skill.Farming, 0b110)
      "Firemaking" in test(Skill.Firemaking, 0b111)
      "Fishing" in test(Skill.Fishing, 0b1000)
      "Fletching" in test(Skill.Fletching, 0b1001)
      "Herblore" in test(Skill.Herblore, 0b1010)
      "Hitpoints" in test(Skill.Hitpoints, 0b1011)
      "Hunter" in test(Skill.Hunter, 0b1100)
      "Magic" in test(Skill.Magic, 0b1101)
      "Mining" in test(Skill.Mining, 0b1110)
      "Prayer" in test(Skill.Prayer, 0b1111)
      "Ranged" in test(Skill.Ranged, 0b10000)
      "Runecraft" in test(Skill.Runecraft, 0b10001)
      "Slayer" in test(Skill.Slayer, 0b10010)
      "Smithing" in test(Skill.Smithing, 0b10011)
      "Strength" in test(Skill.Strength, 0b10100)
      "Thieving" in test(Skill.Thieving, 0b10101)
      "Woodcutting" in test(Skill.Woodcutting, 0b10110)
    }
  }
}
