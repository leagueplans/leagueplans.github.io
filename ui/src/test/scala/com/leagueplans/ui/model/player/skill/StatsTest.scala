package com.leagueplans.ui.model.player.skill

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import org.scalatest.Assertion

final class StatsTest extends CodecSpec {
  "Stats" - {
    final case class Wrapper(s: Stats)
    given Encoder[Wrapper] = Encoder.derived
    given Decoder[Wrapper] = Decoder.derived

    val rangedEnc = Encoder.encode(Skill.Ranged).getBytes
    val farmingEnc = Encoder.encode(Skill.Farming).getBytes

    val exp1 = Exp(737627)
    val exp1Enc = Encoder.encode(exp1).getBytes

    val exp2 = Exp(18247.6)
    val exp2Enc = Encoder.encode(exp2).getBytes

    "encoding values to and decoding values from an expected encoding" - {
      def test(skills: Map[Skill, Exp], expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(
          Wrapper(Stats(skills)),
          Decoder.decodeMessage,
          expectedEncoding
        )

      "An empty map" in test(Map.empty, Array.empty)

      "A single stat map" in test(
        Map(Skill.Ranged -> exp1),
        Array[Byte](0b100, 0b1011, 0b100, 0b100) ++ rangedEnc ++
          Array[Byte](0b1000) ++ exp1Enc
      )

      "A multi-stat map" in test(
        Map(Skill.Ranged -> exp1, Skill.Farming -> exp2),
        Array[Byte](0b100, 0b1011, 0b100, 0b100) ++ rangedEnc ++
          Array[Byte](0b1000) ++ exp1Enc ++
          Array[Byte](0b100, 0b1010, 0b100, 0b100) ++ farmingEnc ++
          Array[Byte](0b1000) ++ exp2Enc
      )
    }
  }
}
