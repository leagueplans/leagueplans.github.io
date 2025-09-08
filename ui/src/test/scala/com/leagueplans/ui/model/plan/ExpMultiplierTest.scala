package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import org.scalatest.Assertion

final class ExpMultiplierTest extends CodecSpec {
  "ExpMultiplier" - {
    "Condition" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(condition: ExpMultiplier.Condition, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(condition, Decoder.decodeMessage, expectedEncoding)

        "AssociatedSkillLevel" in test(
          ExpMultiplier.Condition.AssociatedSkillLevel(45),
          Array[Byte](0, 0, 0b1100, 0b10, 0) ++ Encoder.encode(45).getBytes
        )
        
        "CombatLevel" in test(
          ExpMultiplier.Condition.CombatLevel(120),
          Array[Byte](0, 0b1, 0b1100, 0b11, 0) ++ Encoder.encode(120).getBytes
        )
        
        "TotalLevel" in test(
          ExpMultiplier.Condition.TotalLevel(1683),
          Array[Byte](0, 0b10, 0b1100, 0b11, 0) ++ Encoder.encode(1683).getBytes
        )
        
        "LeaguePoints" in test(
          ExpMultiplier.Condition.LeaguePoints(5400),
          Array[Byte](0, 0b11, 0b1100, 0b11, 0) ++ Encoder.encode(5400).getBytes
        )
        
        "LeagueTasks" in test(
          ExpMultiplier.Condition.LeagueTasks(164),
          Array[Byte](0, 0b100, 0b1100, 0b11, 0) ++ Encoder.encode(164).getBytes
        )
      }
    }
    
    "encoding values to and decoding values from an expected encoding" in {
      val firstThreshold = 5 -> ExpMultiplier.Condition.LeaguePoints(350)
      val secondThreshold = 10 -> ExpMultiplier.Condition.LeagueTasks(90)
      
      testRoundTripSerialisation(
        ExpMultiplier(
          Set(Skill.Magic),
          base = 2,
          thresholds = List(firstThreshold, secondThreshold)
        ),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b100) ++ Encoder.encode(Skill.Magic).getBytes ++
          Array[Byte](0b1000) ++ Encoder.encode(2).getBytes ++
          Array[Byte](0b10100, 0b1011) ++ Encoder.encode(firstThreshold).getBytes ++
          Array[Byte](0b10100, 0b1011) ++ Encoder.encode(secondThreshold).getBytes
      )
    }
  }
}
