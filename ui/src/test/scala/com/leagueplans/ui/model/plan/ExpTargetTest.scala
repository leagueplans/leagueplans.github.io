package com.leagueplans.ui.model.plan

import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.Assertion

final class ExpTargetTest extends CodecSpec {
  "ExpTarget" - {
    "encoding values to and decoding values from an expected encoding" - {
      def test(target: ExpTarget, expectedEncoding: Array[Byte]): Assertion =
        testRoundTripSerialisation(target, Decoder.decodeMessage, expectedEncoding)

      "AtLevel" in {
        val level = Level.L60
        val levelEnc = Encoder.encode(level).getBytes
        test(
          ExpTarget.AtLevel(level),
          Array[Byte](0, 0, 0b1100, (2 + levelEnc.length).toByte, 0b100, levelEnc.length.toByte) ++ levelEnc
        )
      }

      "AtExp" in {
        val exp = Exp(1000)
        val expEnc = Encoder.encode(exp).getBytes
        test(
          ExpTarget.AtExp(exp),
          Array[Byte](0, 0b1, 0b1100, (1 + expEnc.length).toByte, 0) ++ expEnc
        )
      }
    }
  }
}
