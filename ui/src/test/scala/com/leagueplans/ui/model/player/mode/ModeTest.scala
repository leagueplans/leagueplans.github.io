package com.leagueplans.ui.model.player.mode

import com.leagueplans.codec.CharacterEncodings as CE
import com.leagueplans.codec.CharacterEncodings.*
import com.leagueplans.codec.codecs.CodecSpec
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import org.scalatest.Assertion

final class ModeTest extends CodecSpec {
  "Mode" - {
    "League" - {
      "encoding values to and decoding values from an expected encoding" - {
        def test(league: Mode.League, expectedEncoding: Array[Byte]): Assertion =
          testRoundTripSerialisation(league, Decoder.decodeLen, expectedEncoding)

        "LeaguesI" in test(
          LeaguesI,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `1`)
        )

        "LeaguesII" in test(
          LeaguesII,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `2`)
        )

        "LeaguesIII" in test(
          LeaguesIII,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `3`)
        )

        "LeaguesIV" in test(
          LeaguesIV,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `4`)
        )

        "LeaguesV" in test(
          LeaguesV,
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `5`)
        )
      }

      "decoding should fail for an unexpected encoding" in(
        Decoder.decodeLen[Mode.League](
          Array(`l`, `e`, CE.`a`, `g`, `u`, `e`, `s`, `-`, `6`)
        ).left.value shouldBe a[DecodingFailure]
      )
    }
  }
}
