package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.Skill
import org.scalatest.Assertion

final class EffectListTest extends CodecSpec {
  "EffectList" - {
    final case class Wrapper(es: EffectList)
    given Encoder[Wrapper] = Encoder.derived
    given Decoder[Wrapper] = Decoder.derived

    val effect1 = Effect.CompleteQuest(10)
    val effect1Enc = Encoder.encode(effect1).getBytes

    val effect2 = Effect.UnlockSkill(Skill.Cooking)
    val effect2Enc = Encoder.encode(effect2).getBytes

    "encoding values to and decoding values from an expected encoding" - {
      def test(effects: List[Effect], expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(
          Wrapper(EffectList(effects)),
          Decoder.decodeMessage,
          expectedEncoding
        )

      "An empty list" in test(List.empty, Array.empty)

      "A single element list" in test(
        List(effect1),
        Array[Byte](0b100, 0b110) ++ effect1Enc
      )

      "A multi-element list" in test(
        List(effect1, effect2),
        Array[Byte](0b100, 0b110) ++ effect1Enc ++
          Array[Byte](0b100, 0b1010) ++ effect2Enc
      )
    }
  }
}
