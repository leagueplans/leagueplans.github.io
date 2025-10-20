package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import org.scalatest.Assertion

final class ExpMultiplierTest extends CodecSpec {
  "ExpMultiplier" - {
    "Kind" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(kind: ExpMultiplier.Kind, discriminant: Byte): Assertion =
          testRoundTripSerialisation(
            kind,
            Decoder.decodeMessage,
            Array(0, discriminant, 0b1100, 0)
          )

        "Additive" in test(ExpMultiplier.Kind.Additive, 0)
        "Multiplicative" in test(ExpMultiplier.Kind.Multiplicative, 0b1)
      }
    }

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

        "GridAxis" in test(
          ExpMultiplier.Condition.LeagueTasks(164),
          Array[Byte](0, 0b100, 0b1100, 0b11, 0) ++ Encoder.encode(164).getBytes
        )

        "GridTile" in test(
          ExpMultiplier.Condition.LeagueTasks(164),
          Array[Byte](0, 0b100, 0b1100, 0b11, 0) ++ Encoder.encode(164).getBytes
        )
      }
    }

    "GridAxisDirection" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(direction: ExpMultiplier.GridAxisDirection, discriminant: Byte): Assertion =
          testRoundTripSerialisation(
            direction,
            Decoder.decodeMessage,
            Array(0, discriminant, 0b1100, 0)
          )

        "Column" in test(ExpMultiplier.GridAxisDirection.Column, 0)
        "Row" in test(ExpMultiplier.GridAxisDirection.Row, 0b1)
      }
    }
    
    "encoding values to and decoding values from an expected encoding" in {
      val firstThreshold = 5 -> ExpMultiplier.Condition.LeaguePoints(350)
      val secondThreshold = 10 -> ExpMultiplier.Condition.LeagueTasks(90)
      
      testRoundTripSerialisation(
        ExpMultiplier(
          Set(Skill.Magic),
          ExpMultiplier.Kind.Multiplicative,
          base = 2,
          thresholds = List(firstThreshold, secondThreshold)
        ),
        Decoder.decodeMessage,
        Array[Byte](0b100, 0b100) ++ Encoder.encode(Skill.Magic).getBytes ++
          Array[Byte](0b1100, 0b100) ++ Encoder.encode(ExpMultiplier.Kind.Multiplicative).getBytes ++
          Array[Byte](0b10000) ++ Encoder.encode(2).getBytes ++
          Array[Byte](0b11100, 0b1011) ++ Encoder.encode(firstThreshold).getBytes ++
          Array[Byte](0b11100, 0b1011) ++ Encoder.encode(secondThreshold).getBytes
      )
    }
  }
}
